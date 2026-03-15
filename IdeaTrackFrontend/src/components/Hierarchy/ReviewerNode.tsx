import { CheckCircle2, XCircle, Clock, Beaker } from 'lucide-react';
import { ProfileAvatar } from '../../utils/profileImageHandler';
import type { HierarchyNodeDTO } from './HierarchyTypes';

interface ReviewerNodeProps {
  node: HierarchyNodeDTO;
  onOpenProfile: () => void;
}

const StatusIcon = ({ decision }: { decision: string }) => {
  switch (decision) {
    case 'ACCEPTED':
      return <CheckCircle2 size={16} className="text-success" />;
    case 'REJECTED':
      return <XCircle size={16} className="text-danger" />;
    case 'REFINE':
      return <Beaker size={16} className="text-warning" />;
    case 'PENDING':
    default:
      return <Clock size={16} className="text-warning" />;
  }
};

function getDecisionStyles(decision: string | null | undefined) {
  // Handle both reviewer decisions (PENDING, ACCEPTED, REJECTED, REFINE) and admin decisions (APPROVED, REJECTED)
  switch (decision) {
    case 'APPROVED':
    case 'ACCEPTED':
      return {
        bg: 'rgba(25, 135, 84, 0.05)',
        border: '2px solid var(--bs-success)',
        textColor: 'var(--bs-success)'
      };
    case 'REJECTED':
      return {
        bg: 'rgba(220, 53, 69, 0.05)',
        border: '2px solid var(--bs-danger)',
        textColor: 'var(--bs-danger)'
      };
    case 'REFINE':
      return {
        bg: 'rgba(255, 193, 7, 0.05)',
        border: '2px solid var(--bs-warning)',
        textColor: 'var(--bs-warning)'
      };
    case 'PENDING':
    default:
      return {
        bg: 'rgba(255, 193, 7, 0.05)',
        border: '2px solid var(--bs-warning)',
        textColor: 'var(--bs-warning)'
      };
  }
}

function formatDecisionDate(dateString: string | null): string {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
  });
}

export const ReviewerNode = ({ node, onOpenProfile }:ReviewerNodeProps) => {
  const { bg, border, textColor } = getDecisionStyles(node.decision);

  return (
    <div
      onClick={onOpenProfile}
      className="card cursor-pointer"
      style={{
        width: 280,
        flexShrink: 0,
        backgroundColor: bg,
        border: border,
        borderRadius: '14px',
        transition: 'all 0.3s ease',
        cursor: 'pointer',
        boxShadow: '0 2px 12px rgba(67, 24, 255, 0.07)',
        overflow: 'hidden'
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-4px)';
        e.currentTarget.style.boxShadow = '0 8px 28px rgba(67, 24, 255, 0.14)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = '';
        e.currentTarget.style.boxShadow = '0 2px 12px rgba(67, 24, 255, 0.07)';
      }}
      role="button"
      aria-label={`Reviewer ${node.reviewerName || ''} card`}
    >
      {/* Colored top accent bar */}
      <div
        style={{
          height: 4,
          background: textColor,
          width: '100%'
        }}
      />

      <div className="d-flex justify-content-between align-items-start mb-3 p-4 pb-2">
        <div className="d-flex align-items-center gap-3 flex-grow-1">
          <ProfileAvatar profileUrl={node.profileUrl} userName={node.reviewerName || 'R'} size={40} className="rounded-3" />
          <div className="text-truncate">
            <h6 className="text-truncate mb-1 fw-bold" style={{ maxWidth: 150 }}>
              {node.reviewerName}
            </h6>
            <small className="text-secondary fw-500">{node.role}</small>
          </div>
        </div>
        <StatusIcon decision={node.decision} />
      </div>

      {/* Decision Status Box */}
      <div className="px-4 pb-3">
        <div
          className="px-3 py-2 rounded-2 small text-center fw-bold"
          style={{
            backgroundColor: `${textColor}15`,
            color: textColor,
            border: `1.5px solid ${textColor}`
          }}
        >
          {node.decision}
        </div>
        {node.decisionAt && (
          <small className="d-block text-secondary text-center mt-2" style={{ fontSize: '11px' }}>
            {formatDecisionDate(node.decisionAt)}
          </small>
        )}
      </div>

      <div className="px-4 pb-4">
        <div className="p-3 small rounded-2" style={{ fontStyle: 'italic', minHeight: 60, backgroundColor: 'rgba(67, 24, 255, 0.04)', border: '1px solid rgba(67, 24, 255, 0.1)' }}>
          {node.feedback ? `"${node.feedback}"` : 'Pending review...'}
        </div>

        <div className="d-flex justify-content-between align-items-center mt-3">
          {node.totalXp && (
            <small className="fw-bold" style={{ color: '#4318FF' }}>{node.totalXp} XP</small>
          )}
          <small className="text-secondary" style={{ fontSize: '11px' }}>View Profile →</small>
        </div>
      </div>
    </div>
  );
};