import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { IIdea } from './IIdea';
import { deleteIdea as DeleteIdea } from '../../utils/ideaApi';
import { ThumbsUp, Eye, Trash2 } from 'lucide-react';
import ConfirmationModal from '../ConfirmationModal/ConfirmationModal';
import { useShowToast } from '../../hooks/useShowToast';

type Props = {
  idea: IIdea;
  onView?: (idea: IIdea) => void;
  onDeleteSuccess?: (id: number) => void;
};

const STATUS_COLORS: Record<string, { bg: string; color: string }> = {
  DRAFT:        { bg: '#FFF3CD', color: '#92600A' },
  SUBMITTED:    { bg: '#E0E7FF', color: '#4318FF' },
  UNDER_REVIEW: { bg: '#DBEAFE', color: '#1D4ED8' },
  ACCEPTED:     { bg: '#D1FAE5', color: '#065F46' },
  APPROVED:     { bg: '#D1FAE5', color: '#065F46' },
  REJECTED:     { bg: '#FEE2E2', color: '#B91C1C' },
};

const CATEGORY_COLORS = [
  '#4318FF', '#38B2AC', '#ED8936', '#48BB78', '#E53E3E', '#805AD5', '#D69E2E',
];

function getCategoryColor(name?: string) {
  if (!name) return CATEGORY_COLORS[0];
  let hash = 0;
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + hash * 31;
  return CATEGORY_COLORS[Math.abs(hash) % CATEGORY_COLORS.length];
}

const IdeaCard: React.FC<Props> = ({ idea, onView, onDeleteSuccess }) => {
  const navigate = useNavigate();
  const toast = useShowToast();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const date = new Date(idea.createdAt);
  const dateStr = date.toLocaleString(undefined, { month: 'short', day: 'numeric' });

  const statusStyle = STATUS_COLORS[idea.ideaStatus ?? ''] ?? { bg: '#F3F4F6', color: '#6B7280' };
  const catColor = getCategoryColor(idea.category?.name);

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    setConfirmOpen(true);
  };

  const doDelete = async () => {
    try {
      await DeleteIdea(idea.ideaId);
      toast.success('Idea deleted successfully');
      onDeleteSuccess?.(idea.ideaId);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || err?.message || 'Failed to delete idea');
    }
  };

  const handleView = () => {
    if (onView) {
      onView(idea);
    } else {
      navigate(`/my-idea/${idea.ideaId}`);
    }
  };

  return (
    <>
    <div
      style={{
        background: 'white',
        borderRadius: 16,
        overflow: 'hidden',
        boxShadow: '0 2px 12px rgba(67,24,255,0.07)',
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
        transition: 'transform 0.2s, box-shadow 0.2s',
        cursor: 'pointer',
      }}
      onMouseEnter={e => {
        (e.currentTarget as HTMLDivElement).style.transform = 'translateY(-4px)';
        (e.currentTarget as HTMLDivElement).style.boxShadow = '0 8px 28px rgba(67,24,255,0.14)';
      }}
      onMouseLeave={e => {
        (e.currentTarget as HTMLDivElement).style.transform = '';
        (e.currentTarget as HTMLDivElement).style.boxShadow = '0 2px 12px rgba(67,24,255,0.07)';
      }}
    >
      {/* Colored top strip */}
      <div style={{ height: 5, background: catColor }} />

      <div style={{ padding: '20px 20px 16px', display: 'flex', flexDirection: 'column', flex: 1 }}>
        
        {/* Category + Status section (Vertical Stack) */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', gap: 6, marginBottom: 12 }}>
          <span style={{
            background: catColor + '18',
            color: catColor,
            borderRadius: 20,
            padding: '3px 12px',
            fontSize: 12,
            fontWeight: 600,
          }}>
            {idea.category?.name ?? 'General'}
          </span>
          <span style={{
            background: statusStyle.bg,
            color: statusStyle.color,
            borderRadius: 20,
            padding: '3px 12px',
            fontSize: 12,
            fontWeight: 600,
          }}>
            {idea.ideaStatus}
          </span>
        </div>

        {/* Title */}
        <h5 style={{
          margin: 0, fontWeight: 700, fontSize: 15, color: '#1B254B',
          lineHeight: 1.4, marginBottom: 8,
          display: '-webkit-box', WebkitLineClamp: 2,
          WebkitBoxOrient: 'vertical', overflow: 'hidden',
        }}>
          {idea.title}
        </h5>

        {/* Author + date */}
        <div style={{ fontSize: 12, color: '#A3AED0', marginBottom: 'auto' }}>
          by <strong style={{ color: '#4A5568' }}>{idea.author?.displayName}</strong> · {dateStr}
        </div>

        {/* Footer */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginTop: 16, paddingTop: 14,
          borderTop: '1px solid #F4F7FE',
        }}>
          <span style={{
            display: 'flex', alignItems: 'center', gap: 5,
            background: '#FFF8EC', color: '#D97706',
            borderRadius: 20, padding: '4px 12px', fontSize: 13, fontWeight: 600,
          }}>
            <ThumbsUp size={13} /> {idea.votes?.upvotes ?? 0}
          </span>

          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={handleView}
              style={{
                background: '#EFF3FF', color: '#4318FF', border: 'none',
                borderRadius: 20, padding: '6px 16px', fontSize: 13, fontWeight: 600,
                cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5,
              }}
            >
              <Eye size={13} /> View
            </button>
            <button
              onClick={handleDelete}
              style={{
                background: '#FFF0F0', color: '#EE5D50', border: 'none',
                borderRadius: 20, padding: '6px 12px', fontSize: 13, fontWeight: 600,
                cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4,
              }}
            >
              <Trash2 size={13} />
            </button>
          </div>
        </div>
      </div>
    </div>

    <ConfirmationModal
      isOpen={confirmOpen}
      title="Delete Idea"
      message="Are you sure you want to delete this idea? This action cannot be undone."
      confirmText="Delete"
      cancelText="Cancel"
      isDangerous
      onConfirm={async () => { setConfirmOpen(false); await doDelete(); }}
      onCancel={() => setConfirmOpen(false)}
    />
    </>
  );
};

export default IdeaCard;