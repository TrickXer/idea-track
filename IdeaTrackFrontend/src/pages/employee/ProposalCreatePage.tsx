import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Link } from 'lucide-react';
import {
  convertIdeaToProposal,
  type ObjectiveCreation,
  type ProofMeta,
} from '../../utils/proposalApi';
import { getIdeaDetails } from '../../utils/ideaApi';

const MAX_BUDGET_LIMIT = 300000;

type ObjectiveDraft = Omit<ObjectiveCreation, 'proof'> & {
  proofPath: string;
  proofContentType?: ProofMeta['contentType'];
  proofFileName?: string;
};

type IdeaMeta = {
  ideaId?: number;
  title?: string;
  problemStatement?: string;
  description?: string;
  categoryName?: string;
  createdAt?: string;
  tags?: string[];
  thumbnailURL?: string;
  upvotes?: number;
  downvotes?: number;
  authorName?: string;
  ideaStatus?: string;
};

function deriveNameFromUrlOrText(input: string): string {
  const trimmed = (input || '').trim();
  try {
    const u = new URL(trimmed);
    const last = (u.pathname.split('/').pop() || '').trim();
    return last || 'proof.pdf';
  } catch {
    if (!trimmed) return 'proof.pdf';
    return /\.[a-z0-9]+$/i.test(trimmed) ? trimmed : `${trimmed}.pdf`;
  }
}

export default function ProposalConvertPage() {
  const { ideaId } = useParams();
  const navigate = useNavigate();
  const parsedIdeaId = useMemo(() => Number(ideaId), [ideaId]);

  const [form, setForm] = useState({
    userId: '',
    budget: '0',
    timeLineStart: '',
    timeLineEnd: '',
    objectives: [
      { objectiveSeq: 1, title: '', description: '', mandatory: false, proofPath: '' },
    ] as ObjectiveDraft[],
  });

  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');
  const [ok, setOk] = useState('');

  // Confirm modal (for Create Proposal)
  const [showConfirm, setShowConfirm] = useState(false);
  const [pendingPayload, setPendingPayload] = useState<any>(null);

  // ===== NEW: Idea meta state (+ load error) =====
  const [ideaMeta, setIdeaMeta] = useState<IdeaMeta | null>(null);
  const [ideaLoadError, setIdeaLoadError] = useState('');
  const [userName, setUserName] = useState('');

  // Load user (id + name)
  useEffect(() => {
    try {
      const raw = localStorage.getItem('user-profile') || '{}';
      const profile = JSON.parse(raw);
      const uid = profile?.userId ? String(profile.userId) : '';
      const uname = profile?.name || '';
      setForm((f) => ({ ...f, userId: uid || f.userId }));
      setUserName(uname);
    } catch {
      /* ignore */
    }
  }, []);

  // ===== NEW: Load idea details =====
  useEffect(() => {
    (async () => {
      setIdeaLoadError('');
      setIdeaMeta(null);

      if (!parsedIdeaId || Number.isNaN(parsedIdeaId)) return;

      try {
        const raw = localStorage.getItem('user-profile') || '{}';
        const profile = JSON.parse(raw);
        const currentUserId = profile?.userId;

        const res = await getIdeaDetails(parsedIdeaId, currentUserId);
        const data = res?.data || {};

        setIdeaMeta({
          ideaId: data?.ideaId ?? data?.id ?? parsedIdeaId,
          title: data?.title ?? data?.ideaTitle ?? '',
          problemStatement: data?.problemStatement ?? data?.problem ?? '',
          description: data?.description ?? '',
          categoryName: data?.category?.name ?? data?.categoryName ?? '',
          createdAt: data?.createdAt ?? '',
          tags:
            typeof data?.tag === 'string'
              ? data.tag
                  .split(',')
                  .map((t: string) => t.trim())
                  .filter(Boolean)
              : Array.isArray(data?.tags)
              ? data.tags
              : [],
          thumbnailURL: data?.thumbnailURL ?? data?.attachmentUrl ?? '',
          upvotes: data?.votes?.upvotes ?? data?.upvotes ?? 0,
          downvotes: data?.votes?.downvotes ?? data?.downvotes ?? 0,
          authorName: data?.author?.displayName ?? data?.authorName ?? '',
          ideaStatus: data?.ideaStatus ?? data?.status ?? '',
        });
      } catch (e: any) {
        const msg =
          e?.response?.data?.message || e?.message || 'Failed to load idea details.';
        setIdeaLoadError(msg);
      }
    })();
  }, [parsedIdeaId]);

  // ===== Validations =====
  const validDates = useMemo(() => {
    if (!form.timeLineStart || !form.timeLineEnd) return false;
    return new Date(form.timeLineEnd) >= new Date(form.timeLineStart);
  }, [form.timeLineStart, form.timeLineEnd]);

  const budgetNumber = useMemo(() => Number(form.budget), [form.budget]);
  const budgetValid = useMemo(
    () =>
      Number.isFinite(budgetNumber) &&
      budgetNumber >= 0 &&
      budgetNumber < MAX_BUDGET_LIMIT,
    [budgetNumber]
  );

  const allObjectivesValid = useMemo(() => {
    if (form.objectives.length === 0) return false;
    const seqs = form.objectives.map((o) => Number(o.objectiveSeq));
    const uniqueSeqs = new Set(seqs);
    if (seqs.some((s) => !Number.isFinite(s) || s < 1)) return false;
    if (uniqueSeqs.size !== seqs.length) return false;

    for (const o of form.objectives) {
      const titleOk = o.title.trim().length > 0 && o.title.trim().length <= 150;
      const descOk = o.description.trim().length > 0 && o.description.trim().length <= 2000;
      if (!(titleOk && descOk)) return false;
    }
    return true;
  }, [form.objectives]);

  // Master validity you already used to disable buttons
  const basicValid = useMemo(() => {
    const uid = Number(form.userId);
    return (
      !Number.isNaN(uid) &&
      uid > 0 &&
      form.timeLineStart &&
      form.timeLineEnd &&
      validDates &&
      budgetValid &&
      allObjectivesValid
    );
  }, [form, validDates, budgetValid, allObjectivesValid]);

  // ===== Handlers =====
  const changeField = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const changeObjectiveField = (
    idx: number,
    field: keyof ObjectiveDraft,
    value: any
  ) => {
    setForm((f) => {
      const next = [...f.objectives];
      next[idx] = { ...next[idx], [field]: value };
      return { ...f, objectives: next };
    });
  };

  // Build payload (used by both actions)
  const buildPayload = () => ({
    userId: Number(form.userId),
    budget: Number(form.budget),
    timeLineStart: form.timeLineStart,
    timeLineEnd: form.timeLineEnd,
    objectives: form.objectives.map<ObjectiveCreation>((o) => {
      const rawPath = (o.proofPath || '').trim();
      const safePath = rawPath.length > 0 ? rawPath : 'proof.pdf';
      let fileName = deriveNameFromUrlOrText(safePath);
      if (!fileName.toLowerCase().endsWith('.pdf')) fileName = `${fileName}.pdf`;
      const contentType: ProofMeta['contentType'] = 'application/pdf';
      const sizeBytes = 1024;
      return {
        objectiveSeq: Number(o.objectiveSeq),
        title: o.title.trim(),
        description: o.description.trim(),
        mandatory: false,
        proof: { fileName, filePath: safePath, contentType, sizeBytes } as any,
      } as any;
    }),
  });

  // --- Save as Draft ---
  const onSaveDraft = async () => {
    setErr('');
    setOk('');
    if (!ideaId || Number.isNaN(parsedIdeaId)) {
      setErr('Invalid idea id in URL.');
      return;
    }
    if (!basicValid) {
      setErr('Please complete required fields before saving as draft.');
      return;
    }
    try {
      setBusy(true);
      const data = await convertIdeaToProposal(parsedIdeaId, buildPayload() as any);
      setOk('Draft created successfully.');
      if (data?.proposalId) {
        navigate(`/employee/proposals/${data.proposalId}/edit`, { replace: true });
      }
    } catch (e: any) {
      setErr(e?.response?.data?.message || e?.message || 'Failed to create draft');
    } finally {
      setBusy(false);
    }
  };

  // --- Create Proposal ---
  const onSubmitCreate = () => {
    setErr('');
    setOk('');
    if (!ideaId || Number.isNaN(parsedIdeaId)) {
      setErr('Invalid idea id in URL.');
      return;
    }
    if (!basicValid) {
      setErr('Please complete required fields.');
      return;
    }
    setPendingPayload(buildPayload());
    setShowConfirm(true);
  };

  const actuallyCreateProposal = async () => {
    if (!pendingPayload) return;
    setBusy(true);
    setErr('');
    setOk('');
    try {
      const data = await convertIdeaToProposal(parsedIdeaId, pendingPayload as any);
      setOk('Proposal created successfully.');
      setShowConfirm(false);
      setPendingPayload(null);
      if (data?.proposalId) {
        navigate(`/employee/proposals/${data.proposalId}/edit`, { replace: true });
      }
    } catch (e: any) {
      setErr(e?.response?.data?.message || e?.message || 'Failed to create proposal');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container my-4">
      <h3 className="mb-3">Convert Accepted Idea → Proposal</h3>

      {/* ===== NEW: Idea Details Panel ===== */}
      <div className="card border-0 shadow-sm rounded-4 mb-3">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-start">
            <div>
              <div className="fw-bold">
                Submitting proposal for Idea #{ideaId}
                {ideaMeta?.title ? (
                  <>
                    {' '}
                    — <span className="text-dark">{ideaMeta.title}</span>
                  </>
                ) : null}
              </div>
              {ideaLoadError && (
                <small className="text-danger d-block mt-1">
                  Couldn't load idea details: {ideaLoadError}
                </small>
              )}
            </div>
            <div className="text-end">
              <span className="badge bg-primary fs-6">
                User ID: {form.userId || '—'}
              </span>
              {userName && (
                <div className="small text-muted mt-1">User: {userName}</div>
              )}
            </div>
          </div>

          {ideaMeta && !ideaLoadError && (
            <div className="mt-3">
              <div className="row g-3">
                <div className="col-md-4">
                  <div className="small text-muted">Category</div>
                  <div className="fw-semibold">{ideaMeta.categoryName || '—'}</div>
                </div>
                <div className="col-md-4">
                  <div className="small text-muted">Status</div>
                  <div className="fw-semibold">{ideaMeta.ideaStatus || '—'}</div>
                </div>
                <div className="col-md-4">
                  <div className="small text-muted">Created</div>
                  <div className="fw-semibold">
                    {ideaMeta.createdAt
                      ? new Date(ideaMeta.createdAt).toLocaleString()
                      : '—'}
                  </div>
                </div>
              </div>

              <div className="row g-3 mt-1">
                <div className="col-md-4">
                  <div className="small text-muted">Author</div>
                  <div className="fw-semibold">{ideaMeta.authorName || '—'}</div>
                </div>
                <div className="col-md-8">
                  <div className="small text-muted">Tags</div>
                  <div>
                    {(ideaMeta.tags || []).length ? (
                      <div className="d-flex flex-wrap gap-2">
                        {ideaMeta.tags!.map((t, i) => (
                          <span
                            key={i}
                            className="badge bg-light text-primary border border-primary"
                          >
                            {t}
                          </span>
                        ))}
                      </div>
                    ) : (
                      '—'
                    )}
                  </div>
                </div>
              </div>

              <div className="row g-3 mt-1">
                <div className="col-md-4">
                  <div className="small text-muted">Upvotes / Downvotes</div>
                  <div className="fw-semibold">
                    {ideaMeta.upvotes ?? 0} / {ideaMeta.downvotes ?? 0}
                  </div>
                </div>
                <div className="col-md-8">
                  {ideaMeta.thumbnailURL && (
                    <div>
                      <div className="small text-muted">Attachment</div>
                      <a
                        href={ideaMeta.thumbnailURL}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn btn-sm btn-outline-primary rounded-pill"
                      >
                        <Link size={13} className="me-1" /> View Attachment
                      </a>
                    </div>
                  )}
                </div>
              </div>

              <hr />

              <div className="mb-2">
                <div className="small text-muted">Problem Statement</div>
                <div className="text-secondary" style={{ whiteSpace: 'pre-wrap' }}>
                  {ideaMeta.problemStatement || '—'}
                </div>
              </div>

              <div>
                <div className="small text-muted">Description</div>
                <div className="text-secondary" style={{ whiteSpace: 'pre-wrap' }}>
                  {ideaMeta.description || '—'}
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {err && <div className="alert alert-danger">{err}</div>}
      {ok && <div className="alert alert-success">{ok}</div>}

      {/* ===== Form ===== */}
      <div className="card">
        <div className="card-header">Idea #{ideaId}</div>
        <div className="card-body">
          {/* No <form onSubmit> to avoid submit collisions */}
          <div className="row g-3">
            <div className="col-md-3">
              <label className="form-label">User ID</label>
              <input
                type="number"
                name="userId"
                className="form-control"
                min={1}
                value={form.userId}
                onChange={changeField}
                readOnly
                disabled
                required
              />
            </div>

            <div className="col-md-3">
              <label className="form-label">Budget</label>
              <input
                type="number"
                name="budget"
                className={`form-control ${
                  form.budget !== '' && !budgetValid ? 'is-invalid' : ''
                }`}
                min={0}
                max={MAX_BUDGET_LIMIT - 1}
                step="0.01"
                value={form.budget}
                onChange={changeField}
                required
              />
              {!budgetValid && (
                <div className="invalid-feedback">
                  Budget must be ≥ 0 and &lt; {MAX_BUDGET_LIMIT}.
                </div>
              )}
            </div>

            <div className="col-md-3">
              <label className="form-label">Timeline Start</label>
              <input
                type="date"
                name="timeLineStart"
                className="form-control"
                value={form.timeLineStart}
                onChange={changeField}
                required
              />
            </div>

            <div className="col-md-3">
              <label className="form-label">Timeline End</label>
              <input
                type="date"
                name="timeLineEnd"
                className={`form-control ${
                  form.timeLineEnd && !validDates ? 'is-invalid' : ''
                }`}
                value={form.timeLineEnd}
                onChange={changeField}
                required
              />
              <div className="invalid-feedback">
                End date must be on or after start date.
              </div>
            </div>
          </div>

          <hr className="my-4" />

          {/* Objectives */}
          {form.objectives.map((o, idx) => (
            <div className="border rounded p-3 mb-3" key={idx}>
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
                    onChange={(e) => changeObjectiveField(idx, 'title', e.target.value)}
                    required
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Description</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    maxLength={2000}
                    value={o.description}
                    onChange={(e) =>
                      changeObjectiveField(idx, 'description', e.target.value)
                    }
                    required
                  />
                </div>
                <div className="col-12">
                  <label className="form-label">Stored file path (optional)</label>
                  <input
                    type="text"
                    className="form-control"
                    value={o.proofPath}
                    onChange={(e) =>
                      changeObjectiveField(idx, 'proofPath', e.target.value)
                    }
                  />
                </div>
              </div>
            </div>
          ))}

          {/* Actions */}
          <div className="mt-3 d-flex gap-2">
            <button
              type="button"
              className="btn btn-warning"
              onClick={onSaveDraft}
              disabled={busy || !basicValid}
              data-testid="btn-save-draft"
              title={!basicValid ? 'Complete all required fields to proceed' : undefined}
            >
              {busy ? 'Saving…' : 'Save as Draft'}
            </button>

            <button
              type="button"
              className="btn btn-primary"
              onClick={onSubmitCreate}
              disabled={busy || !basicValid}
              data-testid="btn-create-proposal"
              title={!basicValid ? 'Complete all required fields to proceed' : undefined}
            >
              {busy ? 'Creating…' : 'Create Proposal'}
            </button>
          </div>

          {err && <div className="alert alert-danger mt-3">{err}</div>}
          {ok && <div className="alert alert-success mt-3">{ok}</div>}
        </div>
      </div>

      {/* Confirmation modal */}
      {showConfirm && pendingPayload && (
        <div
          className="position-fixed top-0 start-0 w-100 h-100"
          style={{ background: 'rgba(0,0,0,0.4)', zIndex: 1050 }}
          role="dialog"
          aria-modal="true"
          onClick={() => !busy && setShowConfirm(false)}
        >
          <div
            className="d-flex align-items-center justify-content-center h-100"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="card shadow" style={{ maxWidth: 900, width: '95%' }}>
              <div className="card-header d-flex justify-content-between align-items-center">
                <strong>Confirm Proposal Creation</strong>
                <button
                  type="button"
                  className="btn-close"
                  onClick={() => !busy && setShowConfirm(false)}
                />
              </div>
              <div className="card-body">
                <div className="mb-2">
                  <strong>User ID:</strong> {pendingPayload.userId}
                </div>
                <div className="mb-2">
                  <strong>Budget:</strong> {pendingPayload.budget}
                </div>
                <div className="mb-2">
                  <strong>Timeline:</strong> {pendingPayload.timeLineStart} →{' '}
                  {pendingPayload.timeLineEnd}
                </div>
                <div className="mt-3">
                  <h6 className="text-uppercase text-muted mb-2">Objectives</h6>
                  <ol className="mb-0">
                    {pendingPayload.objectives.map((o: any, i: number) => (
                      <li key={i} className="mb-2">
                        <div>
                          <strong>Seq:</strong> {o.objectiveSeq}
                        </div>
                        <div>
                          <strong>Title:</strong> {o.title}
                        </div>
                        <div className="text-muted" style={{ whiteSpace: 'pre-wrap' }}>
                          {o.description}
                        </div>
                      </li>
                    ))}
                  </ol>
                </div>
              </div>
              <div className="card-footer d-flex justify-content-end gap-2">
                <button
                  className="btn btn-outline-secondary"
                  type="button"
                  onClick={() => setShowConfirm(false)}
                  disabled={busy}
                >
                  Cancel
                </button>
                <button
                  className="btn btn-primary"
                  type="button"
                  onClick={actuallyCreateProposal}
                  disabled={busy}
                >
                  {busy ? 'Submitting…' : 'Confirm & Submit'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}