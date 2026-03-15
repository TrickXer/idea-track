import React, { useMemo, useState, useEffect } from 'react';
import restApi from '../../utils/restApi';
import ConfirmationModal from '../ConfirmationModal/ConfirmationModal';
import { useShowToast } from '../../hooks/useShowToast';

type Props = {
  proposalId: number;
  className?: string;
  onSubmitted?: (proposalId: number) => void;
  onDeleted?: (proposalId: number) => void;
};

type UpdateForm = {
  budget: string;
  timeLineStart: string;
  timeLineEnd: string;
  objectives: string;
  objectivesProof: string;
};

type SubmitForm = {
  justification: string;
  objectives: string;
  objectivesProof: string;
};

const initialUpdate: UpdateForm = {
  budget: '',
  timeLineStart: '',
  timeLineEnd: '',
  objectives: '',
  objectivesProof: '',
};

const initialSubmit: SubmitForm = {
  justification: '',
  objectives: '',
  objectivesProof: '',
};

export default function DraftProposalEditor({
  proposalId,
  className = '',
  onSubmitted,
  onDeleted,
}: Props) {
  const [updateForm, setUpdateForm] = useState<UpdateForm>(initialUpdate);
  const [submitForm, setSubmitForm] = useState<SubmitForm>(initialSubmit);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);
  const toast = useShowToast();

  // ------------------------------
  // LOAD EXISTING DRAFT
  // ------------------------------
  useEffect(() => {
    async function loadDraft() {
      try {
        const resp = await restApi.get(`/api/proposals/${proposalId}`);
        const p = resp.data;

        setUpdateForm({
          budget: p.budget?.toString() || '',
          timeLineStart: p.timeLineStart || '',
          timeLineEnd: p.timeLineEnd || '',
          objectives: p.objectives || '',
          objectivesProof: p.objectivesProof || '',
        });

        // ALSO PRE-FILL SUBMIT OBJECTIVES (if needed)
        setSubmitForm((f) => ({
          ...f,
          objectives: p.objectives || '',
          objectivesProof: p.objectivesProof || '',
        }));
      } catch (e: any) {
        console.error(e);
        setErr("Failed to load draft proposal.");
      }
    }

    loadDraft();
  }, [proposalId]);

  const onChangeUpdate = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setUpdateForm((f) => ({ ...f, [name]: value }));
  };

  const onChangeSubmit = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setSubmitForm((f) => ({ ...f, [name]: value }));
  };

  const validDates = useMemo(() => {
    if (!updateForm.timeLineStart || !updateForm.timeLineEnd) return true;
    return new Date(updateForm.timeLineEnd) >= new Date(updateForm.timeLineStart);
  }, [updateForm.timeLineStart, updateForm.timeLineEnd]);

  const canSave = useMemo(() => {
    return Object.values(updateForm).some((v) => v && v.trim() !== '');
  }, [updateForm]);

  const doUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setMsg('');
    setErr('');

    try {
      const payload: Record<string, any> = {
        budget: updateForm.budget ? Number(updateForm.budget) : undefined,
        timeLineStart: updateForm.timeLineStart || undefined,
        timeLineEnd: updateForm.timeLineEnd || undefined,
        objectives: updateForm.objectives || undefined,
        objectivesProof: updateForm.objectivesProof || undefined,
      };

      const cleaned = Object.fromEntries(
        Object.entries(payload).filter(([, v]) => v !== undefined)
      );

      await restApi.patch(`/api/proposals/updateProposal/${proposalId}`, cleaned);
      setMsg('Draft updated successfully.');
    } catch (e: any) {
      setErr(e?.message || 'Update failed');
    } finally {
      setBusy(false);
    }
  };

  const doDelete = async () => {
    setConfirmDeleteOpen(false);
    setBusy(true);
    setMsg('');
    setErr('');

    try {
      await restApi.delete(`/api/proposals/deleteProposal/${proposalId}`);
      toast.success('Draft deleted.');
      if (onDeleted) onDeleted(proposalId);
    } catch (e: any) {
      const errMsg = e?.message || 'Delete failed';
      setErr(errMsg);
      toast.error(errMsg);
    } finally {
      setBusy(false);
    }
  };

  const doSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setMsg('');
    setErr('');

    try {
      const payload = {
        justification: submitForm.justification,
        objectives: submitForm.objectives.trim(),
        objectivesProof: submitForm.objectivesProof?.trim() || undefined,
      };

      await restApi.post(`/api/proposals/${proposalId}/submit`, payload);

      setMsg('Proposal submitted successfully.');
      if (onSubmitted) onSubmitted(proposalId);
    } catch (e: any) {
      setErr(e?.message || 'Submit failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={`container my-4 ${className}`}>
      <h4>Draft Proposal Editor (ID: {proposalId})</h4>

      {msg && <div className="alert alert-success">{msg}</div>}
      {err && <div className="alert alert-danger">{err}</div>}

      {/* UPDATE SECTION */}
      <div className="card mb-3">
        <div className="card-header">Update Draft</div>
        <div className="card-body">
          <form onSubmit={doUpdate}>
            <div className="row g-3">
              <div className="col-md-3">
                <label className="form-label">Budget</label>
                <input
                  type="number"
                  className="form-control"
                  name="budget"
                  min={0}
                  step="0.01"
                  value={updateForm.budget}
                  onChange={onChangeUpdate}
                />
              </div>

              <div className="col-md-3">
                <label className="form-label">Timeline Start</label>
                <input
                  type="date"
                  className="form-control"
                  name="timeLineStart"
                  value={updateForm.timeLineStart}
                  onChange={onChangeUpdate}
                />
              </div>

              <div className="col-md-3">
                <label className="form-label">Timeline End</label>
                <input
                  type="date"
                  name="timeLineEnd"
                  className={`form-control ${updateForm.timeLineEnd && !validDates ? 'is-invalid' : ''}`}
                  value={updateForm.timeLineEnd}
                  onChange={onChangeUpdate}
                />
              </div>

              <div className="col-12">
                <label className="form-label">Objectives</label>
                <textarea
                  className="form-control"
                  name="objectives"
                  rows={3}
                  value={updateForm.objectives}
                  onChange={onChangeUpdate}
                />
              </div>

              <div className="col-12">
                <label className="form-label">Objectives Proof</label>
                <textarea
                  className="form-control"
                  name="objectivesProof"
                  rows={2}
                  value={updateForm.objectivesProof}
                  onChange={onChangeUpdate}
                />
              </div>
            </div>

            <div className="mt-3 d-flex gap-2">
              <button className="btn btn-primary" type="submit" disabled={!canSave || busy || !validDates}>
                {busy ? 'Saving…' : 'Save'}
              </button>

              <button className="btn btn-outline-danger" type="button" onClick={() => setConfirmDeleteOpen(true)} disabled={busy}>
                Delete Draft
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* SUBMIT SECTION */}
      <div className="card">
        <div className="card-header">Submit Draft → Project Proposal</div>
        <div className="card-body">
          <form onSubmit={doSubmit}>

            <div className="mb-3">
              <label className="form-label">Objectives</label>
              <textarea
                className="form-control"
                name="objectives"
                rows={3}
                value={submitForm.objectives}
                onChange={onChangeSubmit}
                required
              />
            </div>

            <div className="mb-3">
              <label className="form-label">Proof for Objectives (optional)</label>
              <textarea
                className="form-control"
                name="objectivesProof"
                rows={2}
                value={submitForm.objectivesProof}
                onChange={onChangeSubmit}
              />
            </div>

            <div className="mb-3">
              <label className="form-label">Justification / Notes</label>
              <textarea
                className="form-control"
                name="justification"
                rows={2}
                value={submitForm.justification}
                onChange={onChangeSubmit}
                required
              />
            </div>

            <button className="btn btn-success" type="submit" disabled={busy}>
              {busy ? 'Submitting…' : 'Submit'}
            </button>

          </form>
        </div>
      </div>

      <ConfirmationModal
        isOpen={confirmDeleteOpen}
        title="Delete Draft"
        message="Delete this draft? This is a soft delete."
        confirmText="Delete"
        cancelText="Cancel"
        isDangerous
        isLoading={busy}
        onConfirm={doDelete}
        onCancel={() => setConfirmDeleteOpen(false)}
      />
    </div>
  );
}