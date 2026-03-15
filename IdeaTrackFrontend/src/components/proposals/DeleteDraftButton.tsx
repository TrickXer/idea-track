import { useState } from 'react';
import { deleteDraftProposal } from '../../utils/proposalApi';
import ConfirmationModal from '../ConfirmationModal/ConfirmationModal';
import { useShowToast } from '../../hooks/useShowToast';

type Props = {
  proposalId: number;
  onDeleted?: (proposalId: number) => void; // callback after success
  className?: string;
  label?: string;
};

export default function DeleteDraftButton({
  proposalId,
  onDeleted,
  className = '',
  label = 'Delete Draft'
}: Props) {
  const [busy, setBusy] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const toast = useShowToast();

  const onClick = () => {
    setConfirmOpen(true);
  };

  const doDelete = async () => {
    setConfirmOpen(false);
    setBusy(true);
    try {
      await deleteDraftProposal(proposalId);
      if (onDeleted) onDeleted(proposalId);
    } catch (e: any) {
      const msg =
        e?.response?.data?.message ||
        e?.response?.data?.error ||
        e?.message ||
        'Delete failed';
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={className}>
      <button
        type="button"
        className="btn btn-outline-danger"
        onClick={onClick}
        disabled={busy}
        title="Soft delete draft (DRAFT only)"
      >
        {busy ? 'Deleting…' : label}
      </button>

      <ConfirmationModal
        isOpen={confirmOpen}
        title="Delete Draft"
        message="Delete this draft? This is a soft delete and only allowed for DRAFT proposals."
        confirmText="Delete"
        cancelText="Cancel"
        isDangerous
        isLoading={busy}
        onConfirm={doDelete}
        onCancel={() => setConfirmOpen(false)}
      />
    </div>
  );
}