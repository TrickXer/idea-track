import { X, Mail, Phone, Info, Star, CheckCircle2, XCircle, Clock } from 'lucide-react';
import { ProfileAvatar } from '../../utils/profileImageHandler';
import type { UserProfileData } from './HierarchyTypes';

interface UserProfileModalProps {
  user: UserProfileData | null;
  onClose: () => void;
  nodeData?: any; // HierarchyNodeDTO with decision info
}

const StatusIcon = ({ decision }: { decision: string }) => {
  switch (decision) {
    case 'ACCEPTED':
    case 'APPROVED':
      return <CheckCircle2 size={16} className="text-success" />;
    case 'REJECTED':
      return <XCircle size={16} className="text-danger" />;
    case 'REFINE':
      return <span className="badge bg-warning text-dark small">REFINE</span>;
    case 'PENDING':
    default:
      return <Clock size={16} className="text-warning" />;
  }
};

function getDecisionColor(decision: string) {
  switch (decision) {
    case 'ACCEPTED':
    case 'APPROVED':
      return 'var(--bs-success)';
    case 'REJECTED':
      return 'var(--bs-danger)';
    case 'REFINE':
    case 'PENDING':
      return 'var(--bs-warning)';
    default:
      return 'var(--bs-secondary)';
  }
}

export const UserProfileModal = ({ user, nodeData, onClose }:UserProfileModalProps) => {
  if (!user) return null;

  const decisionColor = nodeData ? getDecisionColor(nodeData.decision) : undefined;

  return (
    <div 
      className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center p-4"
      style={{
        background: 'rgba(0, 0, 0, 0.5)',
        zIndex: 1000,
        display: 'block',
        backdropFilter: 'blur(4px)'
      }}
      onClick={onClose}
    >
      <div 
        className="card overflow-hidden position-relative w-100"
        style={{
          maxWidth: '520px',
          borderRadius: '18px',
          border: 'none',
          boxShadow: '0 20px 60px rgba(67, 24, 255, 0.2)'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header Gradient */}
        <div 
          className="h-25 position-relative"
          style={{ 
            background: 'linear-gradient(135deg, #4318FF 0%, #6c5ce7 100%)', 
            height: '120px',
            overflow: 'hidden'
          }}
        >
          {/* Decorative circles */}
          <div style={{
            position: 'absolute',
            width: '200px',
            height: '200px',
            background: 'rgba(255, 255, 255, 0.1)',
            borderRadius: '50%',
            top: '-50px',
            right: '-50px'
          }} />
        </div>
        
        <button 
          onClick={onClose}
          className="btn position-absolute top-0 end-0 m-3 p-2 rounded-circle"
          style={{
            background: 'rgba(255, 255, 255, 0.95)',
            border: 'none',
            zIndex: 10,
            width: '40px',
            height: '40px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'all 0.2s'
          }}
          onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
          onMouseLeave={(e) => e.currentTarget.style.transform = ''}
        >
          <X size={20} className="text-dark" />
        </button>

        <div className="p-5 pt-3">
          {/* Avatar and User Info - Side by side layout */}
          <div className="d-flex gap-4 mb-4">
            <div className="mt-n5 flex-shrink-0">
              <div style={{
                border: '4px solid white',
                boxShadow: '0 8px 24px rgba(67, 24, 255, 0.15)',
                borderRadius: '1rem',
                display: 'inline-block'
              }}>
                <ProfileAvatar profileUrl={user.profileUrl} userName={user.name} size={100} className="rounded-3" />
              </div>
            </div>
            <div className="d-flex flex-column justify-content-center">
              {/* Name and Title */}
              <h3 className="h5 fw-bold mb-2" style={{ color: '#1B254B' }}>
                {user.name}
              </h3>
              <small className="fw-bold text-uppercase mb-3 d-block" style={{ color: '#4318FF', letterSpacing: '0.5px', fontSize: '10px' }}>
                {user.role} • {user.departmentName}
              </small>
              {user.level && (
                <div 
                  className="d-flex align-items-center gap-2 px-3 py-2 rounded-pill small fw-bold text-white"
                  style={{ background: 'linear-gradient(135deg, #4318FF, #6c5ce7)', width: 'fit-content' }}
                >
                  <Star size={13} className="fill-current" />
                  Level {user.level}
                </div>
              )}
            </div>
          </div>

          {/* Decision Status Section - only if nodeData provided and has decision */}
          {nodeData && nodeData.decision && (
            <div 
              className="p-4 mb-4 rounded-3"
              style={{
                backgroundColor: `${decisionColor}12`,
                border: `2px solid ${decisionColor}`,
                transition: 'all 0.2s'
              }}
            >
              <div className="d-flex align-items-center justify-content-between mb-2">
                <div>
                  <small className="fw-bold text-uppercase d-block" style={{ color: '#A3AED0', fontSize: '10px', letterSpacing: '0.5px' }}>
                    Current Decision
                  </small>
                  <p className="mb-0 fw-bold h6" style={{ color: decisionColor }}>
                    {nodeData.decision}
                  </p>
                </div>
                <StatusIcon decision={nodeData.decision} />
              </div>
              {nodeData.decisionAt && (
                <small className="d-block" style={{ color: '#A3AED0', fontSize: '11px' }}>
                  Decided: {new Date(nodeData.decisionAt).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                  })}
                </small>
              )}
            </div>
          )}

          {/* XP Progress */}
          {user.totalXp !== undefined && (
            <div className="mb-4 p-3 rounded-3" style={{ backgroundColor: '#EFF3FF' }}>
              <div className="d-flex justify-content-between mb-3">
                <small className="fw-bold text-uppercase" style={{ color: '#A3AED0', fontSize: '10px', letterSpacing: '0.5px' }}>
                  Experience Points
                </small>
                <small className="fw-bold" style={{ color: '#4318FF' }}>
                  {user.totalXp} / 5000 XP
                </small>
              </div>
              <div className="progress rounded-pill" style={{ height: '8px', backgroundColor: '#D4DAEE' }}>
                <div 
                  className="progress-bar rounded-pill"
                  style={{ 
                    width: `${Math.min((user.totalXp / 5000) * 100, 100)}%`,
                    background: 'linear-gradient(90deg, #4318FF, #6c5ce7)',
                    transition: 'width 0.3s ease'
                  }}
                />
              </div>
            </div>
          )}

          {/* Biography */}
          {user.bio && user.bio.trim() && (
            <div className="p-4 mb-4 rounded-3" style={{ backgroundColor: '#F4F7FE', border: '1px solid #E9EDF7' }}>
              <p className="small fw-bold text-uppercase mb-2 d-flex align-items-center gap-2" style={{ color: '#A3AED0', fontSize: '10px', letterSpacing: '0.5px' }}>
                <Info size={13} />
                Biography
              </p>
              <small className="text-secondary lh-lg d-block">
                "{user.bio}"
              </small>
            </div>
          )}

          {/* Contact Section */}
          <div className="d-flex flex-column gap-3">
            <div className="d-flex align-items-center gap-3">
              <button
                onClick={() => window.location.href = `mailto:${user.email}`}
                className="btn d-flex align-items-center gap-2 fw-semibold text-white rounded-2"
                style={{
                  background: 'linear-gradient(135deg, #4318FF, #6c5ce7)',
                  border: 'none',
                  padding: '8px 16px',
                  fontSize: '13px',
                  transition: 'all 0.2s'
                }}
                onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-2px)'}
                onMouseLeave={(e) => e.currentTarget.style.transform = ''}
                title="Send email"
              >
                <Mail size={15} />
                Send Mail
              </button>
              <small className="text-secondary" style={{ fontSize: '12px' }}>{user.email}</small>
            </div>
            <div className="d-flex align-items-center gap-3">
              <div className="p-2 rounded-2" style={{ backgroundColor: '#EFF3FF' }}>
                <Phone size={15} style={{ color: '#4318FF' }} />
              </div>
              <small className="text-secondary" style={{ fontSize: '12px' }}>{user.phoneNo}</small>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
