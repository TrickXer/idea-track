// src/pages/Hierarchy.tsx
import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { ChevronDown } from 'lucide-react';
import { ProfileAvatar } from '../../utils/profileImageHandler';
import { ReviewerNode } from '../../components/Hierarchy/ReviewerNode';
import { UserProfileModal } from '../../components/Hierarchy/UserProfileModal';
import { Timeline } from '../../components/Hierarchy/Timeline';
import { getIdeaHierarchy } from '../../utils/profileHierarchy';
import type { IdeaHierarchyDTO, UserProfileData } from '../../components/Hierarchy/HierarchyTypes';
import styles from './IdeaHierarchy.module.css';

const getStatusStyling = (status: string) => {
  switch (status) {
    case 'UNDERREVIEW':
      return {
        bg: 'var(--bs-info-bg-subtle)',
        border: 'var(--bs-info)',
        text: 'var(--bs-info)'
      };
    case 'ACCEPTED':
    case 'APPROVED':
      return {
        bg: 'var(--bs-success-bg-subtle)',
        border: 'var(--bs-success)',
        text: 'var(--bs-success)'
      };
    case 'REJECTED':
      return {
        bg: 'var(--bs-danger-bg-subtle)',
        border: 'var(--bs-danger)',
        text: 'var(--bs-danger)'
      };
    default:
      return {
        bg: 'var(--bs-light)',
        border: 'var(--bs-border-color)',
        text: 'var(--bs-body-color)'
      };
  }
};

const IdeaHierarchy = () => {
  const { ideaId: ideaIdParam } = useParams<{ ideaId: string }>();
  const [data, setData] = useState<IdeaHierarchyDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedStages, setExpandedStages] = useState<Record<string, boolean>>({});
  const [selectedUser, setSelectedUser] = useState<UserProfileData | null>(null);
  const [selectedNodeData, setSelectedNodeData] = useState<any>(null);

  useEffect(() => {
    const fetchHierarchyData = async () => {
      try {
        setLoading(true);
        setError(null);

        const ideaId = ideaIdParam ? Number(ideaIdParam) : null;
        if (!ideaId) {
          setError('No idea ID provided.');
          setLoading(false);
          return;
        }
        const hierarchyData = await getIdeaHierarchy(ideaId);
        setData(hierarchyData);

        // Initialize all stages as closed
        const keys = Object.keys(hierarchyData.nodesByStage).sort((a, b) => Number(a) - Number(b));
        const init: Record<string, boolean> = {};
        keys.forEach(k => (init[k] = false));
        setExpandedStages(init);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load hierarchy');
        console.error('Error loading hierarchy:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchHierarchyData();
  }, [ideaIdParam]);

  if (loading) {
    return (
      <div className="min-vh-100 d-flex justify-content-center align-items-center">
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading idea hierarchy...</p>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="min-vh-100 d-flex justify-content-center align-items-center">
        <div className="alert alert-danger" role="alert">
          <strong>Error:</strong> {error || 'Failed to load hierarchy. Please try again.'}
        </div>
      </div>
    );
  }

  const toggleStage = (stage: string) => {
    setExpandedStages(prev => ({ ...prev, [stage]: !prev[stage] }));
  };

  const handleOpenProfile = (person: any, type: 'reviewer' | 'admin') => {
    const profile: UserProfileData = {
      userId: type === 'admin' ? person.adminUserId : person.reviewerId,
      name: type === 'admin' ? person.adminName : person.reviewerName,
      email:
        person.email ||
        person.adminEmail ||
        `${(type === 'admin' ? person.adminName : person.reviewerName || 'user')
          .toLowerCase()
          .replace(/\s+/g, '.')}@ideatrack.com`,
      phoneNo: person.phoneNo || person.adminPhoneNo || '+1 (555) 000-0000',
      bio:
        person.bio ||
        person.adminBio ||
        (type === 'admin' ? 'Final authority for product decisions.' : 'Technical reviewer and subject matter expert.'),
      profileUrl: (type === 'admin' ? person.adminProfileUrl : person.profileUrl) || null, // backend uses adminProfileUrl for admin
      role: type === 'admin' ? person.adminRole : person.role,
      departmentName: type === 'admin' ? person.adminDept : person.department,
      totalXp: person.totalXp,
      level: person.level
    };
    setSelectedUser(profile);
    // Pass node data if it's a reviewer
    if (type === 'reviewer') {
      setSelectedNodeData({
        decision: person.decision,
        decisionAt: person.decisionAt
      });
    } else if (type === 'admin') {
      // Pass admin decision data
      setSelectedNodeData({
        decision: person.decision,
        decisionAt: person.decisionAt
      });
    }
  };

  const handleOpenOwnerProfile = () => {
    if (!data) return;
    const owner = data.owner;
    const profile: UserProfileData = {
      userId: owner.ownerUserId,
      name: owner.ownerName,
      email: `${owner.ownerName.toLowerCase().replace(/\s+/g, '.')}@ideatrack.com`, // fallback if backend doesn't return
      phoneNo: '+1 (555) 000-0000', // fallback
      bio: 'Idea originator',
      profileUrl: owner.ownerProfileUrl || null,
      role: owner.ownerRole,
      departmentName: owner.ownerDept,
      totalXp: undefined,
      level: undefined
    };
    setSelectedUser(profile);
    setSelectedNodeData(null);
  };

  const sortedStages = Object.keys(data.nodesByStage).sort((a, b) => parseInt(a) - parseInt(b));
  const statusStyle = getStatusStyling(data.status);

  return (
    <div className="min-vh-100 pb-5">
      <UserProfileModal 
        user={selectedUser} 
        nodeData={selectedNodeData}
        onClose={() => {
          setSelectedUser(null);
          setSelectedNodeData(null);
        }} 
      />

      <main className="container" style={{ paddingTop: '128px' }}>
        {/* IDEA HEADER */}
        <div className="mb-4 pb-3 border-bottom">
          <div className="row align-items-end g-3">
            <div className="col-12 col-md-8">
              <div className="d-flex align-items-center gap-2 mb-3">
                <span className="badge bg-info fw-bold small">
                  Idea #{data.ideaId}
                </span>
                <span className="badge fw-bold small" style={{
                  backgroundColor: statusStyle.bg,
                  color: statusStyle.text,
                  border: `1px solid ${statusStyle.border}`
                }}>
                  {data.status}
                </span>
              </div>
              <h1 className="h3 fw-bold mb-2">
                {data.title}
              </h1>
              <p className="mb-0 text-secondary">
                {data.description}
              </p>
            </div>
          </div>
        </div>

        {/* MAIN GRID */}
        <div className="row gy-5 gx-5">
          {/* Left column: owner → stages → admin */}
          <div className="col-12 col-lg-8 d-flex flex-column align-items-center">
            {/* HIERARCHY TREE CONTAINER */}
            <div className="p-4 w-100 mb-4">
              {/* OWNER card */}
              <div
                onClick={handleOpenOwnerProfile}
                className="card p-4 mb-4 w-100"
                style={{ 
                  cursor: 'pointer',
                  borderRadius: '14px',
                  border: 'none',
                  background: 'white',
                  boxShadow: '0 2px 12px rgba(67, 24, 255, 0.07)',
                  transition: 'all 0.3s ease',
                  overflow: 'hidden'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = '0 8px 28px rgba(67, 24, 255, 0.14)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = '';
                  e.currentTarget.style.boxShadow = '0 2px 12px rgba(67, 24, 255, 0.07)';
                }}
                role="button"
                aria-label="View idea originator profile"
                tabIndex={0}
                onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleOpenOwnerProfile()}
              >
                {/* Colored accent bar */}
                <div style={{
                  height: 4,
                  background: 'linear-gradient(90deg, #4318FF, #6c5ce7)',
                  width: '100%',
                  margin: '-16px -16px 16px -16px'
                }} />
                <div className="d-flex flex-column align-items-center mb-3">
                  <ProfileAvatar profileUrl={data.owner.ownerProfileUrl} userName={data.owner.ownerName || 'O'} size={50} className="rounded-3 mb-3" />
                  <h5 className="fw-bold mb-1 text-center" style={{ color: '#1B254B' }}>
                    {data.owner.ownerName}
                  </h5>
                  <small className="d-block fw-bold text-uppercase text-center" style={{ color: '#4318FF', letterSpacing: '0.1em', fontSize: '10px', marginBottom: '16px' }}>
                    Originator • {data.owner.ownerDept}
                  </small>
                  <div className="p-3 rounded-2 fw-bold h6 mb-0 w-100 text-center" style={{
                    backgroundColor: '#EFF3FF',
                    borderColor: '#4318FF',
                    border: '2px solid',
                    color: '#4318FF'
                  }}>
                    Idea Originator
                  </div>
                  <small className="d-block mt-3 text-secondary text-center" style={{ fontSize: '12px' }}>
                    View Profile →
                  </small>
                </div>
              </div>
              <div style={{ width: '100%' }} onClick={e => e.stopPropagation()}>
              {sortedStages.map((stageKey) => {
                const nodes = data.nodesByStage[stageKey];
                const isExpanded = !!expandedStages[stageKey];

                return (
                  <div key={stageKey} className="d-flex flex-column align-items-center">
                    {/* vertical connector */}
                    <div style={{
                      width: 3,
                      height: 48,
                      background: 'linear-gradient(180deg, #4318FF, rgba(67, 24, 255, 0.3))',
                      borderRadius: 2,
                      margin: '0 auto'
                    }} />
                    <button
                      onClick={() => toggleStage(stageKey)}
                      className="btn d-flex align-items-center justify-content-center gap-3 mb-4 fw-bold"
                      style={{ 
                        margin: '0 auto 1rem',
                        borderRadius: '12px',
                        border: '2px solid #E9EDF7',
                        background: isExpanded ? 'linear-gradient(135deg, #4318FF, #6c5ce7)' : 'white',
                        color: isExpanded ? 'white' : '#1B254B',
                        padding: '12px 24px',
                        transition: 'all 0.3s ease'
                      }}
                      onMouseEnter={(e) => {
                        if (!isExpanded) {
                          e.currentTarget.style.background = '#F4F7FE';
                        }
                      }}
                      onMouseLeave={(e) => {
                        if (!isExpanded) {
                          e.currentTarget.style.background = 'white';
                        }
                      }}
                    >
                      <div className="text-center">
                        <small className="d-block fw-bold" style={{ fontSize: '10px', letterSpacing: '0.5px', textTransform: 'uppercase' }}>
                          Stage
                        </small>
                        <p className="mb-0 fw-bold h6">
                          {stageKey}
                        </p>
                      </div>
                      <ChevronDown
                        size={18}
                        style={{
                          transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
                          transition: 'transform 0.2s'
                        }}
                      />
                    </button>

                    {isExpanded && (
                      <div 
                        className={`${styles.reviewersContainer} mb-3 ${
                          nodes.length === 1 ? styles.reviewersContainerSingleNode : styles.reviewersContainerMultiNode
                        }`}
                      >
                        {nodes.map((node) => (
                          <ReviewerNode
                            key={node.id}
                            node={node}
                            onOpenProfile={() => handleOpenProfile(node, 'reviewer')}
                          />
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}

                {/* bottom connector */}
                <div style={{
                  width: 3,
                  height: 48,
                  background: 'linear-gradient(180deg, rgba(67, 24, 255, 0.3), transparent)',
                  borderRadius: 2,
                  margin: '0 auto'
                }} />
              </div>

              {/* ADMIN card */}
              <div
                onClick={(e) => {
                  e.stopPropagation();
                  handleOpenProfile(data.admin, 'admin');
                }}
                className="card p-4 text-center w-100"
                style={{ 
                  cursor: 'pointer',
                  borderRadius: '14px',
                  border: 'none',
                  background: 'white',
                  boxShadow: '0 2px 12px rgba(67, 24, 255, 0.07)',
                  transition: 'all 0.3s ease',
                  overflow: 'hidden'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = '0 8px 28px rgba(67, 24, 255, 0.14)';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = '';
                  e.currentTarget.style.boxShadow = '0 2px 12px rgba(67, 24, 255, 0.07)';
                }}
                role="button"
                aria-label="View final authority profile"
                tabIndex={0}
                onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && handleOpenProfile(data.admin, 'admin')}
              >
                {/* Colored accent bar */}
                <div style={{
                  height: 4,
                  background: 'linear-gradient(90deg, #4318FF, #6c5ce7)',
                  width: '100%',
                  margin: '-16px -16px 16px -16px'
                }} />
                <div className="d-flex flex-column align-items-center mb-3">
                  <ProfileAvatar profileUrl={data.admin.adminProfileUrl} userName={data.admin.adminName || 'A'} size={50} className="rounded-3 mb-3" />
                  <h5 className="fw-bold mb-1 text-center" style={{ color: '#1B254B' }}>
                    {data.admin.adminName}
                  </h5>
                  <small className="d-block fw-bold text-uppercase text-center" style={{ color: '#4318FF', letterSpacing: '0.1em', fontSize: '10px', marginBottom: '16px' }}>
                    Admin • {data.admin.adminDept}
                  </small>
                </div>

                <div
                  className="p-3 rounded-2 fw-bold h6 mb-0"
                  style={{
                    backgroundColor: data.admin.decision === 'APPROVED' ? 'rgba(5, 150, 105, 0.1)' : data.admin.decision === 'REJECTED' ? 'rgba(220, 53, 69, 0.1)' : '#EFF3FF',
                    borderColor: data.admin.decision === 'APPROVED' ? '#059669' : data.admin.decision === 'REJECTED' ? '#DC2626' : '#4318FF',
                    border: '2px solid',
                    color: data.admin.decision === 'APPROVED' ? '#059669' : data.admin.decision === 'REJECTED' ? '#DC2626' : '#4318FF'
                  }}
                >
                  {data.admin.decision || 'AWAITING DECISION'}
                </div>
                <small className="d-block mt-3 text-secondary" style={{ fontSize: '12px' }}>
                  View Profile →
                </small>
              </div>
            </div>
          </div>

          {/* Right column: Timeline */}
          <div className="col-12 col-lg-4" style={{ opacity: selectedUser ? 0.5 : 1, transition: 'opacity 0.3s ease' }}>
            <Timeline events={data.timeline} offsetTop={120} />
          </div>
        </div>
      </main>
    </div>
  );
};

export default IdeaHierarchy;