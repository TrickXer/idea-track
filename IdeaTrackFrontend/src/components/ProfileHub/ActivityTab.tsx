import React, { useEffect, useMemo, useState } from 'react';
import {
  Filter, History, Lightbulb, MessageSquare, ThumbsUp, TrendingUp, ChevronRight, Info,
} from 'lucide-react';
import {
  getInteractionsPage,
  getInteractionsByTypePage,
} from '../../utils/profileHierarchy';
import type { UserActivityDTO } from './ProfileTypes';
import './ActivityTab.module.css';

type ActivityFilter = 'ALL' | 'COMMENT' | 'VOTE' | 'IDEA_STATUS';

/** Extend DTO so we can display backend extras (ideaId, reason, voteType…) */
type ActivityExtended = UserActivityDTO & {
  id?: number;
  userId?: number;
  ideaId?: number;
  voteType?: string | null;
  savedIdea?: boolean;
  totalAfterChange?: number; // NOTE: retained in type for compatibility, but not rendered
  reason?: string | null;
  updatedAt?: string;
};

const PAGE_SIZE = 10;

// ---------------- Helpers: mapping & formatting ----------------

function mapApiTypeToUI(apiType?: string): UserActivityDTO['activityType'] {
  if (!apiType) return 'SUBMISSION';
  if (apiType === 'CURRENTSTATUS') return 'IDEA_STATUS';
  if (apiType === 'COMMENT') return 'COMMENT';
  if (apiType === 'VOTE') return 'VOTE';
  if (apiType === 'SUBMISSION') return 'SUBMISSION';
  return 'SUBMISSION';
}

function mapFilterToApiType(filter: ActivityFilter): string | undefined {
  if (filter === 'ALL') return undefined;
  if (filter === 'IDEA_STATUS') return 'CURRENTSTATUS';
  return filter; // COMMENT or VOTE map 1:1
}

function computeEventLabel(a: ActivityExtended): string {
  if (a.reason) {
    if (a.reason === 'VOTE') return a.voteType ? `Vote: ${a.voteType}` : 'Vote';
    if (a.reason === 'COMMENT') return 'Comment added';
    if (a.reason === 'PROPOSAL_SUBMITTED') return 'Proposal submitted';
  }
  switch (a.activityType) {
    case 'COMMENT': return 'Comment added';
    case 'VOTE': return a.voteType ? `Vote: ${a.voteType}` : 'Vote';
    case 'IDEA_STATUS': return 'Idea progress';
    case 'SUBMISSION': return 'Submission';
    default: return 'Activity';
  }
}

function normalizeApiRecord(rec: any): ActivityExtended {
  const activityType = mapApiTypeToUI(rec.activityType);
  const delta = typeof rec.delta === 'number' ? rec.delta : 0;
  return {
    id: rec.id,
    userActivityId: rec.id ?? rec.userActivityId ?? Math.random(),
    userId: rec.userId,
    ideaId: rec.ideaId,
    commentText: rec.commentText ?? undefined,
    voteType: rec.voteType ?? null,
    savedIdea: Boolean(rec.savedIdea),
    delta,
    // Kept for compatibility; not rendered anywhere
    totalAfterChange: rec.totalAfterChange ?? undefined,
    reason: rec.reason ?? null,
    activityType,
    event: computeEventLabel({ ...rec, activityType, delta } as ActivityExtended),
    createdAt: rec.createdAt,
    updatedAt: rec.updatedAt,
  };
}

function formatDateTime(iso?: string) {
  if (!iso) return '';
  const d = new Date(iso);
  return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

function snippet(text?: string | null, len = 80) {
  if (!text) return '';
  return text.length > len ? `${text.slice(0, len)}…` : text;
}

// ---------------- Modal (Bootstrap) ----------------

function ActivityDetailsModal({
  activity,
  onClose,
}: {
  activity: ActivityExtended | null;
  onClose: () => void;
}) {
  if (!activity) return null;
  const pos = activity.delta > 0;
  const neg = activity.delta < 0;

  // Determine which date to show - the most recent one
  const getRecentDate = () => {
    if (!activity.createdAt && !activity.updatedAt) return null;
    if (!activity.updatedAt) return { label: 'Created', date: activity.createdAt };
    if (!activity.createdAt) return { label: 'Updated', date: activity.updatedAt };

    const created = new Date(activity.createdAt);
    const updated = new Date(activity.updatedAt);
    return updated > created
      ? { label: 'Updated', date: activity.updatedAt }
      : { label: 'Created', date: activity.createdAt };
  };

  const recentDate = getRecentDate();

  return (
    <>
      <div
        style={{
          position: 'fixed',
          inset: 0,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 1050,
          pointerEvents: 'auto'
        }}
      >
        <div
          style={{
            maxWidth: '500px',
            width: '90%',
            background: 'white',
            borderRadius: 20,
            border: 'none',
            boxShadow: '0px 24px 48px rgba(27, 37, 75, 0.2)',
            overflow: 'hidden',
            pointerEvents: 'auto'
          }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div style={{
            padding: '24px 28px',
            borderBottom: '1px solid #E9EDF7',
            background: 'linear-gradient(135deg, #4318FF 0%, #6C63FF 100%)'
          }}>
            <div style={{ marginBottom: 0 }}>
              <div style={{
                fontSize: 10,
                textTransform: 'uppercase',
                fontWeight: 700,
                letterSpacing: '0.5px',
                color: 'rgba(255, 255, 255, 0.7)',
                marginBottom: 8
              }}>Activity Details</div>
              <h5 style={{
                fontSize: 18,
                fontWeight: 700,
                marginBottom: 0,
                color: 'white'
              }}>{activity.event}</h5>
            </div>
          </div>

          {/* Body */}
          <div style={{ padding: '28px', maxHeight: '60vh', overflowY: 'auto' }}>
            {/* Badges */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 24 }}>
              <span style={{
                background: 'linear-gradient(135deg, rgba(67, 24, 255, 0.15) 0%, rgba(108, 99, 255, 0.1) 100%)',
                color: '#4318FF',
                padding: '8px 14px',
                borderRadius: 10,
                fontSize: 10,
                fontWeight: 700,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                border: '1px solid rgba(67, 24, 255, 0.2)'
              }}>
                {activity.activityType}
              </span>
              {activity.reason && (
                <span style={{
                  background: 'linear-gradient(135deg, rgba(1, 181, 116, 0.15) 0%, rgba(1, 181, 116, 0.1) 100%)',
                  color: '#01B574',
                  padding: '8px 14px',
                  borderRadius: 10,
                  fontSize: 10,
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                  border: '1px solid rgba(1, 181, 116, 0.2)'
                }}>
                  {activity.reason}
                </span>
              )}
              {typeof activity.ideaId === 'number' && (
                <span style={{
                  background: 'linear-gradient(135deg, rgba(255, 159, 64, 0.15) 0%, rgba(255, 193, 7, 0.1) 100%)',
                  color: '#FF9F40',
                  padding: '8px 14px',
                  borderRadius: 10,
                  fontSize: 10,
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                  border: '1px solid rgba(255, 159, 64, 0.2)'
                }}>
                  Idea #{activity.ideaId}
                </span>
              )}
              {activity.voteType && (
                <span style={{
                  background: 'linear-gradient(135deg, rgba(238, 93, 80, 0.15) 0%, rgba(244, 67, 54, 0.1) 100%)',
                  color: '#EE5D50',
                  padding: '8px 14px',
                  borderRadius: 10,
                  fontSize: 10,
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                  border: '1px solid rgba(238, 93, 80, 0.2)'
                }}>
                  {activity.voteType}
                </span>
              )}
            </div>

            {/* Numbers Grid: ONLY Delta displayed (Total After Change removed) */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: 12, marginBottom: 24 }}>
              <div style={{
                background: pos ? 'linear-gradient(135deg, rgba(1, 181, 116, 0.1) 0%, rgba(1, 181, 116, 0.05) 100%)' : neg ? 'linear-gradient(135deg, rgba(238, 93, 80, 0.1) 0%, rgba(238, 93, 80, 0.05) 100%)' : 'linear-gradient(135deg, rgba(108, 99, 255, 0.1) 0%, rgba(108, 99, 255, 0.05) 100%)',
                borderRadius: 12,
                padding: 16,
                textAlign: 'center',
                border: pos ? '1.5px solid rgba(1, 181, 116, 0.3)' : neg ? '1.5px solid rgba(238, 93, 80, 0.3)' : '1.5px solid rgba(108, 99, 255, 0.3)'
              }}>
                <div style={{
                  fontSize: 10,
                  textTransform: 'uppercase',
                  fontWeight: 700,
                  letterSpacing: '0.5px',
                  color: '#A3AED0',
                  marginBottom: 8
                }}>Delta</div>
                <div style={{
                  fontSize: 18,
                  fontWeight: 700,
                  color: pos ? '#01B574' : neg ? '#EE5D50' : '#2B3674'
                }}>
                  {pos ? `+${activity.delta}` : activity.delta}
                </div>
              </div>
            </div>

            {/* Comment */}
            {activity.commentText && (
              <div
                style={{
                  background: 'linear-gradient(135deg, rgba(255, 193, 7, 0.1) 0%, rgba(255, 193, 7, 0.05) 100%)',
                  borderRadius: 12,
                  padding: 16,
                  marginBottom: 24,
                  border: '1.5px solid rgba(255, 193, 7, 0.3)',
                  textAlign: 'center' // 👈 center everything inside
                }}
              >
                <div
                  style={{
                    fontSize: 10,
                    textTransform: 'uppercase',
                    fontWeight: 700,
                    letterSpacing: '0.5px',
                    color: '#A3AED0',
                    marginBottom: 8
                  }}
                >
                  Comment
                </div>
                <div
                  style={{
                    fontSize: 14,
                    color: '#2B3674',
                    fontWeight: 500,
                    display: 'inline-block', // 👈 keeps width tight for neat centering
                    maxWidth: '100%'
                  }}
                >
                  {activity.commentText}
                </div>
              </div>
            )}

            {/* Meta */}
            {recentDate && (
              <div style={{
                background: 'linear-gradient(135deg, rgba(108, 99, 255, 0.1) 0%, rgba(108, 99, 255, 0.05) 100%)',
                borderRadius: 12,
                padding: 16,
                textAlign: 'center',
                border: '1.5px solid rgba(108, 99, 255, 0.3)'
              }}>
                <div style={{
                  fontSize: 10,
                  textTransform: 'uppercase',
                  fontWeight: 700,
                  letterSpacing: '0.5px',
                  color: '#A3AED0',
                  marginBottom: 8
                }}>{recentDate.label}</div>
                <div style={{
                  fontSize: 14,
                  color: '#2B3674',
                  fontWeight: 500
                }}>{formatDateTime(recentDate.date)}</div>
              </div>
            )}
          </div>

          {/* Footer */}
          <div style={{
            padding: '16px 28px',
            borderTop: '1px solid #E9EDF7',
            background: 'linear-gradient(135deg, #F4F7FE 0%, #FFFFFF 100%)',
            display: 'flex',
            justifyContent: 'flex-end'
          }}>
            <button
              type="button"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                onClose();
              }}
              style={{
                background: 'linear-gradient(135deg, #4318FF 0%, #6C63FF 100%)',
                border: 'none',
                color: 'white',
                padding: '10px 24px',
                borderRadius: 8,
                fontWeight: 700,
                cursor: 'pointer',
                fontSize: 14,
                transition: 'all 0.3s ease'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = '#4318FF';
                e.currentTarget.style.color = 'white';
                e.currentTarget.style.borderColor = '#4318FF';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent';
                e.currentTarget.style.color = '#2B3674';
                e.currentTarget.style.borderColor = '#E9EDF7';
              }}
            >
              Close
            </button>
          </div>
        </div>
      </div>
      {/* Backdrop */}
      <div
        style={{
          position: 'fixed',
          inset: 0,
          background: 'rgba(27, 37, 75, 0.5)',
          backdropFilter: 'blur(5px)',
          zIndex: 1040,
          pointerEvents: 'auto'
        }}
        onClick={onClose}
      />
    </>
  );
}

// ---------------- Main ActivityTab (Bootstrap) ----------------

interface ActivityTabProps {
  /** Optional initial list. Component still pages from API. */
  initialActivities?: UserActivityDTO[];
}

export const ActivityTab = ({ initialActivities }:ActivityTabProps) => {
  const [filter, setFilter] = useState<ActivityFilter>('ALL');
  const [items, setItems] = useState<ActivityExtended[]>(
    () =>
      (initialActivities ?? []).map((a) =>
        normalizeApiRecord({
          ...a,
          id: a.userActivityId,
          activityType: a.activityType,
          reason: a.event, // best-effort if parent supplied 'event'
        })
      )
  );
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [selected, setSelected] = useState<ActivityExtended | null>(null);

  const FILTERS: { id: ActivityFilter; label: string; icon: React.ReactNode }[] = [
    { id: 'ALL', label: 'All Interactions', icon: <History size={16} /> },
    { id: 'IDEA_STATUS', label: 'Idea Progress', icon: <Lightbulb size={16} /> },
    { id: 'COMMENT', label: 'Comments', icon: <MessageSquare size={16} /> },
    { id: 'VOTE', label: 'Votes Cast', icon: <ThumbsUp size={16} /> },
  ];

  async function loadPage(pageToLoad: number, replace = false) {
    const apiType = mapFilterToApiType(filter);
    try {
      pageToLoad === 0 ? setIsLoading(true) : setIsLoadingMore(true);

      const res = apiType
        ? await getInteractionsByTypePage(apiType, pageToLoad, PAGE_SIZE)
        : await getInteractionsPage(pageToLoad, PAGE_SIZE);

      // Accept both shapes:
      const pageRecords: any[] =
        Array.isArray(res) ? res : Array.isArray(res.data) ? res.data : res.content ?? [];

      const normalized = pageRecords.map(normalizeApiRecord);

      setItems((prev) => (replace ? normalized : [...prev, ...normalized]));

      const isLast = typeof res.last === 'boolean' ? res.last : normalized.length < PAGE_SIZE;
      setHasMore(!isLast);
      setPage(pageToLoad);
    } catch (err) {
      console.error('Failed to load interactions:', err);
    } finally {
      setIsLoading(false);
      setIsLoadingMore(false);
    }
  }

  // initial + on filter change
  useEffect(() => {
    setItems([]);
    setHasMore(true);
    loadPage(0, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const visible = useMemo(() => items, [items]);

  const iconForActivity = (a: ActivityExtended) => {
    const pos = a.delta > 0;
    const neg = a.delta < 0;
    const colorCls = pos ? 'text-success' : neg ? 'text-danger' : 'text-secondary';
    if (a.activityType === 'COMMENT') return <MessageSquare size={24} className={colorCls} />;
    if (a.activityType === 'VOTE') return <ThumbsUp size={24} className={colorCls} />;
    if (a.activityType === 'IDEA_STATUS') return <Lightbulb size={24} className={colorCls} />;
    return <Info size={24} className={colorCls} />;
  };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '280px 1fr', gap: 28 }}>
      {/* Filter Sidebar */}
      <div style={{ position: 'sticky', top: 100 }}>
        <div style={{
          background: 'white',
          borderRadius: 20,
          padding: 24,
          boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
          border: 'none'
        }}>
          <h4 style={{
            fontSize: 12,
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.5px',
            marginBottom: 16,
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            color: '#4318FF'
          }}>
            <Filter size={14} />
            Filter Activity
          </h4>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {FILTERS.map((btn) => {
              const active = filter === btn.id;
              return (
                <button
                  key={btn.id}
                  onClick={() => { if (!active) setFilter(btn.id); }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '12px 16px',
                    borderRadius: 12,
                    border: active ? 'none' : '1px solid #E9EDF7',
                    background: active ? 'linear-gradient(135deg, #4318FF 0%, #6C63FF 100%)' : 'white',
                    color: active ? 'white' : '#2B3674',
                    cursor: 'pointer',
                    fontWeight: 700,
                    fontSize: 14,
                    transition: 'all 0.3s ease'
                  }}
                  onMouseEnter={(e) => !active && (e.currentTarget.style.background = '#F4F7FE')}
                  onMouseLeave={(e) => !active && (e.currentTarget.style.background = 'white')}
                >
                  <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ color: active ? 'white' : '#4318FF' }}>{btn.icon}</span>
                    {btn.label}
                  </span>
                  <ChevronRight size={14} style={{ opacity: active ? 1 : 0.3 }} />
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* Activity List */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
          <TrendingUp color='#4318FF' size={22} />
          <h3 style={{
            fontSize: 20,
            fontWeight: 700,
            marginBottom: 0,
            color: '#2B3674'
          }}>XP Ledger</h3>
        </div>

        {/* Initial loading */}
        {isLoading && visible.length === 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} style={{
                background: 'white',
                borderRadius: 12,
                padding: 16,
                boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
                height: 100,
                animation: 'pulse 2s infinite'
              }} />
            ))}
          </div>
        )}

        {/* Items */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {visible.map((act) => {
            const pos = act.delta > 0;
            const neg = act.delta < 0;
            return (
              <div
                key={act.userActivityId}
                style={{
                  background: 'white',
                  borderRadius: 12,
                  padding: 16,
                  boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
                  border: '1px solid #E9EDF7',
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  transition: 'all 0.3s ease',
                  cursor: 'pointer'
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.transform = 'translateY(-2px)';
                  e.currentTarget.style.boxShadow = '0px 24px 48px rgba(112, 144, 176, 0.2)';
                  e.currentTarget.style.borderColor = '#4318FF';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.transform = 'translateY(0)';
                  e.currentTarget.style.boxShadow = '0px 18px 40px rgba(112, 144, 176, 0.12)';
                  e.currentTarget.style.borderColor = '#E9EDF7';
                }}
              >
                {/* Left: icon + content */}
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16, flex: 1 }}>
                  <div style={{
                    width: 56,
                    height: 56,
                    minWidth: 56,
                    borderRadius: 12,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: pos ? 'rgba(1, 181, 116, 0.15)' : neg ? 'rgba(238, 93, 80, 0.15)' : 'rgba(67, 24, 255, 0.1)',
                    color: pos ? '#01B574' : neg ? '#EE5D50' : '#4318FF'
                  }}>
                    {iconForActivity(act)}
                  </div>

                  <div style={{ flex: 1 }}>
                    <div style={{ fontWeight: 700, color: '#2B3674', marginBottom: 4 }}>{act.event}</div>
                    <small style={{ color: '#A3AED0', display: 'block', marginBottom: 8, fontSize: 12 }}>{formatDateTime(act.createdAt)}</small>

                    {/* inline details/chips */}
                    <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8, marginTop: 8 }}>
                      {typeof act.ideaId === 'number' && (
                        <span style={{
                          background: 'rgba(108, 99, 255, 0.1)',
                          color: '#6C63FF',
                          padding: '4px 10px',
                          borderRadius: 6,
                          fontSize: 10,
                          fontWeight: 700,
                          textTransform: 'uppercase',
                          letterSpacing: '0.3px'
                        }}>
                          Idea #{act.ideaId}
                        </span>
                      )}
                      {act.reason && (
                        <span style={{
                          background: 'rgba(67, 24, 255, 0.1)',
                          color: '#4318FF',
                          padding: '4px 10px',
                          borderRadius: 6,
                          fontSize: 10,
                          fontWeight: 700,
                          textTransform: 'uppercase',
                          letterSpacing: '0.3px'
                        }}>
                          {act.reason}
                        </span>
                      )}
                      {act.voteType && (
                        <span style={{
                          background: 'rgba(1, 181, 116, 0.1)',
                          color: '#01B574',
                          padding: '4px 10px',
                          borderRadius: 6,
                          fontSize: 10,
                          fontWeight: 700,
                          textTransform: 'uppercase',
                          letterSpacing: '0.3px'
                        }}>
                          {act.voteType}
                        </span>
                      )}
                      {act.commentText && (
                        <small style={{ color: '#A3AED0', fontSize: 12 }}>{snippet(act.commentText)}</small>
                      )}
                    </div>
                  </div>
                </div>

                {/* Right: delta + View */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginLeft: 16 }}>
                  <div style={{
                    fontSize: 16,
                    fontWeight: 700,
                    whiteSpace: 'nowrap',
                    color: pos ? '#01B574' : neg ? '#EE5D50' : '#2B3674'
                  }}>
                    {pos ? `+${act.delta}` : act.delta} XP
                  </div>
                  <button
                    onClick={() => setSelected(act)}
                    style={{
                      background: 'transparent',
                      border: '1px solid #E9EDF7',
                      color: '#2B3674',
                      padding: '8px 16px',
                      borderRadius: 8,
                      fontWeight: 700,
                      fontSize: 12,
                      cursor: 'pointer',
                      transition: 'all 0.3s ease'
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.background = '#4318FF';
                      e.currentTarget.style.color = 'white';
                      e.currentTarget.style.borderColor = '#4318FF';
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.background = 'transparent';
                      e.currentTarget.style.color = '#2B3674';
                      e.currentTarget.style.borderColor = '#E9EDF7';
                    }}
                  >
                    View
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* Empty state */}
        {!isLoading && visible.length === 0 && (
          <div style={{
            background: 'rgba(67, 24, 255, 0.05)',
            border: '1px solid rgba(67, 24, 255, 0.2)',
            borderRadius: 12,
            padding: 20,
            textAlign: 'center',
            color: '#2B3674',
            marginTop: 16
          }}>
            No activity found for this filter.
          </div>
        )}

        {/* Load More */}
        {hasMore && (
          <button
            onClick={() => loadPage(page + 1)}
            disabled={isLoadingMore}
            style={{
              background: 'linear-gradient(135deg, #4318FF 0%, #6C63FF 100%)',
              color: 'white',
              border: 'none',
              width: '100%',
              padding: '14px 24px',
              borderRadius: 12,
              fontWeight: 700,
              textTransform: 'uppercase',
              fontSize: 12,
              marginTop: 24,
              cursor: isLoadingMore ? 'not-allowed' : 'pointer',
              opacity: isLoadingMore ? 0.6 : 1,
              transition: 'all 0.3s ease',
              letterSpacing: '0.5px'
            }}
            onMouseEnter={(e) => !isLoadingMore && (e.currentTarget.style.boxShadow = '0px 6px 16px rgba(67, 24, 255, 0.3)')}
            onMouseLeave={(e) => (e.currentTarget.style.boxShadow = 'none')}
          >
            {isLoadingMore ? 'Loading…' : 'Load More'}
          </button>
        )}
      </div>

      {/* Modal */}
      <ActivityDetailsModal activity={selected} onClose={() => setSelected(null)} />
    </div>
  );
};