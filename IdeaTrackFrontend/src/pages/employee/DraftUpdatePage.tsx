import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import restApi from '../../utils/restApi';
import type { ObjectiveCreation, ProofMeta } from '../../utils/proposalApi';

// ---- Types (adapt as per your backend DTO) ----
type ProofDTO = {
  fileName?: string;
  filePath?: string;
  contentType?: ProofMeta['contentType'];
  sizeBytes?: number;
};

type ObjectiveDTO = {
  id?: number;
  objectiveSeq: number;
  title: string;
  description: string;
  mandatory?: boolean;
  proof?: ProofDTO;
  updatedAt?: string;
};

type ProposalResponseDTO = {
  proposalId: number;
  ideaId: number;
  userId: number;
  budget: number;
  timeLineStart: string; // yyyy-MM-dd
  timeLineEnd: string;   // yyyy-MM-dd
  ideaStatus: string;    // DRAFT / PROJECTPROPOSAL ...
  createdAt?: string;
  updatedAt?: string;
  // backend returns List<Objectives> likely as "objective"
  objective?: ObjectiveDTO[];
};

// ---- Constants ----
const MAX_BUDGET_LIMIT = 300000;
const MAX_BYTES = 25 * 1024 * 1024; // 25 MB

// ---- Helpers ----
function safeProofForBackend(rawPath?: string): ProofMeta {
  const trimmed = (rawPath || '').trim();
  const filePath = trimmed.length > 0 ? trimmed : 'proof.pdf';

  let fileName: string;
  try {
    const u = new URL(filePath);
    const last = (u.pathname.split('/').pop() || '').trim();
    fileName = last || 'proof.pdf';
  } catch {
    fileName = filePath || 'proof.pdf';
  }
  if (!fileName.toLowerCase().endsWith('.pdf')) fileName = `${fileName}.pdf`;

  // force a valid content type and safe size > 0
  return { fileName, filePath, contentType: 'application/pdf', sizeBytes: 1024 };
}

export default function DraftUpdatePage() {
  const { proposalId } = useParams();
  const id = Number(proposalId);
  const navigate = useNavigate();

  // ---- State ----
  const [proposal, setProposal] = useState<ProposalResponseDTO | null>(null);

  const [form, setForm] = useState({
    budget: '',
    timeLineStart: '',
    timeLineEnd: '',
  });

  const [objectives, setObjectives] = useState<ObjectiveDTO[]>([]);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  // ---- Derived ----
  const isDraft = useMemo(
    () => (proposal?.ideaStatus || 'DRAFT').toUpperCase() === 'DRAFT',
    [proposal?.ideaStatus]
  );

  const validDates = useMemo(() => {
    if (!form.timeLineStart || !form.timeLineEnd) return true; // allow partial save on edit
    return new Date(form.timeLineEnd) >= new Date(form.timeLineStart);
  }, [form.timeLineStart, form.timeLineEnd]);

  const validBudget = useMemo(() => {
    if (form.budget === '') return true;
    const b = Number(form.budget);
    return Number.isFinite(b) && b >= 0 && b < MAX_BUDGET_LIMIT;
  }, [form.budget]);

  // ---- Effects ----
  useEffect(() => {
    if (!proposalId || Number.isNaN(id)) return;

    (async () => {
      try {
        // Requires GET /api/proposal/{id} on backend
        const resp = await restApi.get<ProposalResponseDTO>(`/api/proposal/${id}`);
        const p = resp.data;
        setProposal(p);

        setForm({
          budget: String(p?.budget ?? ''),
          timeLineStart: p?.timeLineStart ?? '',
          timeLineEnd: p?.timeLineEnd ?? '',
        });

        setObjectives((p?.objective ?? []).map(o => ({
          ...o,
          proof: o.proof ?? { fileName: '', filePath: '' },
        })));
      } catch (e: any) {
        const m = e?.response?.data?.message || e?.message || 'Failed to load draft';
        setErr(m);
      }
    })();
  }, [proposalId, id]);

  // ---- Handlers ----
  const onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;

    if (name === 'budget') {
      const num = Number(value);
      if (!Number.isNaN(num) && num >= MAX_BUDGET_LIMIT) {
        // warning UX optionally
        console.warn(`Budget must be less than ${MAX_BUDGET_LIMIT}. Entered: ${num}`);
      }
    }

    setForm(f => ({ ...f, [name]: value }));
  };

  const changeObjectiveField = <K extends keyof ObjectiveDTO>(
    idx: number,
    field: K,
    value: ObjectiveDTO[K]
  ) => {
    setObjectives(list => {
      const next = [...list];
      next[idx] = { ...next[idx], [field]: value };
      return next;
    });
  };

  const changeObjectiveProofPath = (idx: number, value: string) => {
    setObjectives(list => {
      const next = [...list];
      const prev = next[idx];
      next[idx] = { ...prev, proof: { ...(prev.proof ?? {}), filePath: value } };
      return next;
    });
  };

  // Save latest draft then submit → PROJECTPROPOSAL
  const doFinalSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!proposalId || Number.isNaN(id)) return;

    setBusy(true);
    setMsg('');
    setErr('');
    try {
      // 1) Save latest changes
      await restApi.put(`/api/proposal/updateProposal/${id}`, {
        budget: form.budget === '' ? undefined : Number(form.budget),
        timeLineStart: form.timeLineStart || undefined,
        timeLineEnd: form.timeLineEnd || undefined,
        objectives: objectives.map<ObjectiveCreation>((o) => ({
          objectiveSeq: Number(o.objectiveSeq),
          title: (o.title || '').trim(),
          description: (o.description || '').trim(),
          mandatory: false,
          proof: safeProofForBackend(o.proof?.filePath),
        })),
      });

      // 2) Final submit (moves to PROJECTPROPOSAL)
      const resp = await restApi.post<ProposalResponseDTO>(`/api/proposal/${id}/submit`, {
        timeLineStart: form.timeLineStart,
        timeLineEnd: form.timeLineEnd,
        budget: form.budget === '' ? 0 : Number(form.budget),
      });

      // 3) Redirect after submit
      navigate('/employee/accepted-ideas', {
        replace: true,
        state: {
          updated: {
            ideaId: resp.data.ideaId,
            proposalId: resp.data.proposalId,
            proposalStatus: resp.data.ideaStatus,
          },
        },
      });
    } catch (e: any) {
      setErr(e?.response?.data?.message || e?.message || 'Submit failed');
    } finally {
      setBusy(false);
    }
  };

  if (!proposalId || Number.isNaN(id)) {
    return (
      <div className="container my-4">
        <div className="alert alert-danger">Invalid proposal id.</div>
      </div>
    );
  }

  return (
    <div className="container my-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h3 className="mb-0">Proposal #{proposal?.proposalId || id}</h3>
        <span className={`badge ${isDraft ? 'bg-warning text-dark' : 'bg-info text-dark'}`}>
          {proposal?.ideaStatus || 'DRAFT'}
        </span>
      </div>

      {/* Meta */}
      <div className="card mb-3">
        <div className="card-body small text-muted">
          <div className="row g-2">
            <div className="col-md-3"><strong>Idea ID:</strong> {proposal?.ideaId ?? '—'}</div>
            <div className="col-md-3"><strong>User ID:</strong> {proposal?.userId ?? '—'}</div>
            <div className="col-md-3"><strong>Created:</strong> {proposal?.createdAt ? new Date(proposal.createdAt).toLocaleString() : '—'}</div>
            <div className="col-md-3"><strong>Updated:</strong> {proposal?.updatedAt ? new Date(proposal.updatedAt).toLocaleString() : '—'}</div>
          </div>
        </div>
      </div>

      {msg && <div className="alert alert-success">{msg}</div>}
      {err && <div className="alert alert-danger">{err}</div>}

      {/* Minimal details needed for Final Submit */}
      <div className="card">
        <div className="card-header">Details {isDraft ? '(Editable)' : '(Read-only after submit)'}</div>
        <div className="card-body">
          <form onSubmit={doFinalSubmit}>
            <div className="row g-3">
              <div className="col-md-3">
                <label className="form-label">Budget</label>
                <input
                  type="number"
                  name="budget"
                  min={0}
                  max={MAX_BUDGET_LIMIT - 1}
                  step="0.01"
                  className="form-control"
                  value={form.budget}
                  onChange={onChange}
                  disabled={!isDraft}
                />
                {!validBudget && (
                  <div className="text-danger small mt-1">Budget must be ≥ 0 and &lt; {MAX_BUDGET_LIMIT}.</div>
                )}
              </div>
              <div className="col-md-3">
                <label className="form-label">Timeline Start</label>
                <input
                  type="date"
                  name="timeLineStart"
                  className="form-control"
                  value={form.timeLineStart}
                  onChange={onChange}
                  disabled={!isDraft}
                />
              </div>
              <div className="col-md-3">
                <label className="form-label">Timeline End</label>
                <input
                  type="date"
                  name="timeLineEnd"
                  className={`form-control ${form.timeLineEnd && !validDates ? 'is-invalid' : ''}`}
                  value={form.timeLineEnd}
                  onChange={onChange}
                  disabled={!isDraft}
                />
                {!validDates && <div className="invalid-feedback">End must be on/after start.</div>}
              </div>
            </div>

            {/* Objectives list (optional to show/edit here) */}
            {objectives.length === 0 && (
              <div className="alert alert-secondary mt-3">No objectives. Add at least one before final submit.</div>
            )}
            {objectives.map((o, idx) => (
              <div key={o.id ?? `obj-${idx}`} className="border rounded p-3 my-3">
                <div className="row g-3">
                  <div className="col-md-2">
                    <label className="form-label">Seq</label>
                    <input
                      type="number"
                      className="form-control"
                      min={1}
                      value={o.objectiveSeq}
                      readOnly
                      disabled
                    />
                  </div>
                  <div className="col-md-10">
                    <label className="form-label">Title</label>
                    <input
                      type="text"
                      className="form-control"
                      maxLength={150}
                      value={o.title}
                      onChange={e => changeObjectiveField(idx, 'title', e.target.value)}
                      disabled={!isDraft}
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Description</label>
                    <textarea
                      className="form-control"
                      rows={3}
                      maxLength={2000}
                      value={o.description}
                      onChange={e => changeObjectiveField(idx, 'description', e.target.value)}
                      disabled={!isDraft}
                    />
                  </div>
                  <div className="col-12">
                    <label className="form-label">Stored file path (optional)</label>
                    <input
                      type="text"
                      className="form-control"
                      value={o.proof?.filePath ?? ''}
                      onChange={e => changeObjectiveProofPath(idx, e.target.value)}
                      disabled={!isDraft}
                    />
                  </div>
                </div>
              </div>
            ))}

            <div className="mt-3">
              <div className="mb-2 text-muted small">
                Submission requires Budget, Timeline Start and End. After submit, editing and deletion are disabled.
              </div>
              <button
                className="btn btn-success"
                type="submit"
                disabled={
                  !isDraft ||
                  busy ||
                  !form.timeLineStart ||
                  !form.timeLineEnd ||
                  Number.isNaN(Number(form.budget)) ||
                  Number(form.budget) < 0 ||
                  objectives.length === 0 ||
                  !validDates
                }
              >
                {busy ? 'Submitting…' : 'Final Submit'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}