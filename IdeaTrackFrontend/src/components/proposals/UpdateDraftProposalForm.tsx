import React, { useMemo, useState } from 'react';
import { updateDraftProposal, type ProposalUpdateRequestDTO, type ProposalResponseDTO } from '../../utils/proposalApi';

type Props = {
  proposalId: number;
  className?: string;
  initial?: {
    budget?: number;
    timeLineStart?: string; // yyyy-MM-dd
    timeLineEnd?: string;   // yyyy-MM-dd
  };
  onUpdated?: (updated: ProposalResponseDTO) => void;
};

export default function UpdateDraftProposalForm({
  proposalId,
  className = '',
  initial,
  onUpdated,
}: Props) {
  const [form, setForm] = useState({
    budget: initial?.budget !== undefined ? String(initial.budget) : '',
    timeLineStart: initial?.timeLineStart ?? '',
    timeLineEnd: initial?.timeLineEnd ?? '',
    // If you later want objectives here, add them in state as well
  });
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  const validDates = useMemo(() => {
    if (!form.timeLineStart || !form.timeLineEnd) return true; // allow partial update
    return new Date(form.timeLineEnd) >= new Date(form.timeLineStart);
  }, [form.timeLineStart, form.timeLineEnd]);

  const validBudget = useMemo(() => {
    if (form.budget === '') return true; // allow partial update
    const b = Number(form.budget);
    return Number.isFinite(b) && b >= 0;
  }, [form.budget]);

  const canSave = useMemo(() => validDates && validBudget, [validDates, validBudget]);

  const change = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSave) return;

    setBusy(true);
    setMsg('');
    setErr('');

    try {
      const payload: ProposalUpdateRequestDTO = {
        budget: form.budget !== '' ? Number(form.budget) : undefined,
        timeLineStart: form.timeLineStart || undefined,
        timeLineEnd: form.timeLineEnd || undefined,
        // objectives: [...], // NOTE: your current backend updateDraft ignores this
      };

      const updated = await updateDraftProposal(proposalId, payload);
      setMsg('Draft updated successfully.');
      if (onUpdated) onUpdated(updated);
    } catch (e: any) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.error ||
        e?.message ||
        'Update failed';
      setErr(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={`card ${className}`}>
      <div className="card-header">Update Draft Proposal (ID: {proposalId})</div>
      <div className="card-body">
        {msg && <div className="alert alert-success">{msg}</div>}
        {err && <div className="alert alert-danger">{err}</div>}

        <form onSubmit={submit}>
          <div className="row g-3">
            <div className="col-md-3">
              <label className="form-label">Budget</label>
              <input
                type="number"
                name="budget"
                className={`form-control ${!validBudget ? 'is-invalid' : ''}`}
                min={0}
                step="0.01"
                value={form.budget}
                onChange={change}
                placeholder="e.g., 50000"
              />
              {!validBudget && (
                <div className="invalid-feedback">Budget must be ≥ 0.</div>
              )}
              <div className="form-text">Leave empty to keep as-is.</div>
            </div>

            <div className="col-md-3">
              <label className="form-label">Timeline Start</label>
              <input
                type="date"
                name="timeLineStart"
                className="form-control"
                value={form.timeLineStart}
                onChange={change}
              />
              <div className="form-text">Leave empty to keep as-is.</div>
            </div>

            <div className="col-md-3">
              <label className="form-label">Timeline End</label>
              <input
                type="date"
                name="timeLineEnd"
                className={`form-control ${form.timeLineEnd && !validDates ? 'is-invalid' : ''}`}
                value={form.timeLineEnd}
                onChange={change}
              />
              {!validDates && (
                <div className="invalid-feedback">End date must be on/after start date.</div>
              )}
              <div className="form-text">Leave empty to keep as-is.</div>
            </div>

            {/* If/when your backend supports updating objectives here, enable this block:
            <div className="col-12">
              <hr className="my-3" />
              <h6 className="mb-2">Objectives (optional)</h6>
              <div className="alert alert-warning mb-0">
                Your current backend <b>updateDraft</b> does not apply <code>objectives</code>.
                Add server-side handling before enabling this section.
              </div>
            </div>
            */}
          </div>

          <div className="mt-3 d-flex gap-2">
            <button className="btn btn-primary" type="submit" disabled={!canSave || busy}>
              {busy ? 'Saving…' : 'Save'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
``