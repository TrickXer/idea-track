import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getAllProposals, getOverdueReviewers, getHealthSummary } from '../../utils/proposalApi';
import type { AdminHealthSummaryDto } from '../../utils/proposalApi';
import {
  Users, ClipboardList, Tag, Zap, UserCheck,
  BarChart2, Heart, Clock, AlertTriangle, UserCog,
  TrendingUp, Activity,
} from 'lucide-react';

type RawProposal = {
  proposalId?: number;
  id?: number;
  title?: string;
  status?: string;
  createdAt?: string;
  [key: string]: any;
};

const STATUS_STYLES: Record<string, { bg: string; color: string }> = {
  APPROVED:        { bg: '#D1FAE5', color: '#065F46' },
  REJECTED:        { bg: '#FEE2E2', color: '#B91C1C' },
  PROJECTPROPOSAL: { bg: '#FEF3C7', color: '#92600A' },
  SUBMITTED:       { bg: '#FEF3C7', color: '#92600A' },
  UNDERREVIEW:     { bg: '#DBEAFE', color: '#1D4ED8' },
};

const AdminDashboard = () => {
  const navigate = useNavigate();

  const [health, setHealth] = useState<AdminHealthSummaryDto | null>(null);
  const [recentProposals, setRecentProposals] = useState<RawProposal[]>([]);
  const [overdueCount, setOverdueCount] = useState<number>(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadAll = async () => {
      setLoading(true);
      try {
        const [healthRes, proposalsRes, overdueRes] = await Promise.allSettled([
          getHealthSummary(),
          getAllProposals({ page: 0, size: 5, sort: 'createdAt,desc' }),
          getOverdueReviewers(),
        ]);

        if (healthRes.status === 'fulfilled') setHealth(healthRes.value);

        if (proposalsRes.status === 'fulfilled') {
          const raw = proposalsRes.value;
          const items: RawProposal[] = Array.isArray(raw)
            ? raw
            : Array.isArray(raw?.content)
            ? raw.content
            : [];
          setRecentProposals(items.slice(0, 5));
        }

        if (overdueRes.status === 'fulfilled') {
          const raw = overdueRes.value;
          const items = Array.isArray(raw) ? raw : Array.isArray(raw?.content) ? raw.content : [];
          setOverdueCount(items.length);
        }
      } catch (e) {
        console.error('AdminDashboard load error', e);
      } finally {
        setLoading(false);
      }
    };
    loadAll();
  }, []);

  const statCards = [
    {
      label: 'Pending Proposals', value: health?.pendingProposals,
      icon: <ClipboardList size={22} />, color: '#4318FF', bg: '#EFF3FF',
      route: '/admin/proposals/pending',
    },
    {
      label: 'Overdue Reviewers', value: overdueCount,
      icon: <AlertTriangle size={22} />, color: '#E53E3E', bg: '#FFF5F5',
      route: '/admin/reviewers-overdue',
    },
    {
      label: 'Total Users', value: health?.totalUsers,
      icon: <Users size={22} />, color: '#38B2AC', bg: '#E6FFFA',
      route: '/admin/users',
    },
    {
      label: 'Active Users', value: health?.activeUsers,
      icon: <Activity size={22} />, color: '#48BB78', bg: '#F0FFF4',
      route: undefined,
    },
  ];

  const quickActions = [
    { label: 'User Management',     desc: 'Add, edit, deactivate users',          icon: <UserCog size={20} />,    color: '#4318FF', route: '/admin/users' },
    { label: 'Proposal Queue',      desc: 'Review submitted proposals',           icon: <ClipboardList size={20} />, color: '#38B2AC', route: '/admin/proposals/pending' },
    { label: 'Categories',          desc: 'Manage idea categories',               icon: <Tag size={20} />,        color: '#ED8936', route: '/admin/categories' },
    { label: 'Bulk Idea Ops',       desc: 'Mass status changes & notifications',  icon: <Zap size={20} />,        color: '#805AD5', route: '/admin/bulk-ideas' },
    { label: 'Reviewer Assignment', desc: 'Assign reviewers to categories',       icon: <UserCheck size={20} />,  color: '#D69E2E', route: '/admin/users?tab=STAGE_ASSIGN' },
    { label: 'Reports & Analytics', desc: 'Generate innovation reports',          icon: <BarChart2 size={20} />,  color: '#E53E3E', route: '/admin/reports' },
    { label: 'Health Summary',      desc: 'System health at a glance',            icon: <Heart size={20} />,      color: '#48BB78', route: '/admin/health' },
    { label: 'Overdue Reviewers',   desc: 'Reviewers with pending tasks',         icon: <Clock size={20} />,      color: '#E53E3E', route: '/admin/reviewers-overdue' },
  ];

  return (
    <div style={{ padding: '0 4px' }}>

      {/* Hero / Header */}
      <div style={{
        background: 'linear-gradient(135deg, #1B254B 0%, #4318FF 100%)',
        borderRadius: 20, padding: '32px 36px',
        color: 'white', marginBottom: 28,
        position: 'relative', overflow: 'hidden',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        flexWrap: 'wrap', gap: 16,
      }}>
        <div style={{ position: 'absolute', right: -60, top: -60, width: 240, height: 240, borderRadius: '50%', background: 'rgba(255,255,255,0.05)' }} />
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
            <TrendingUp size={18} style={{ opacity: 0.8 }} />
            <span style={{ fontSize: 12, opacity: 0.75, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 1 }}>Administration</span>
          </div>
          <h3 style={{ margin: 0, fontWeight: 800, fontSize: 24, marginBottom: 4 }}>Admin Dashboard</h3>
          <p style={{ margin: 0, opacity: 0.75, fontSize: 14 }}>Platform overview &amp; governance controls</p>
        </div>
        <div style={{ display: 'flex', gap: 10, position: 'relative', zIndex: 1, flexWrap: 'wrap' }}>
          <button
            onClick={() => navigate('/dashboard')}
            style={{
              background: 'rgba(255,255,255,0.12)', color: 'white',
              border: '1.5px solid rgba(255,255,255,0.3)',
              borderRadius: 30, padding: '8px 20px', fontWeight: 600, fontSize: 13, cursor: 'pointer',
            }}
          >
            My Ideas
          </button>
          <button
            onClick={() => navigate('/explore')}
            style={{
              background: 'white', color: '#4318FF',
              border: 'none', borderRadius: 30, padding: '8px 20px',
              fontWeight: 700, fontSize: 13, cursor: 'pointer',
            }}
          >
            Idea Wall
          </button>
        </div>
      </div>

      {/* Stat Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 16, marginBottom: 28 }}>
        {statCards.map(stat => (
          <div
            key={stat.label}
            onClick={() => stat.route && navigate(stat.route)}
            style={{
              background: 'white', borderRadius: 16,
              padding: '20px 24px', boxShadow: '0 2px 12px rgba(67,24,255,0.07)',
              display: 'flex', alignItems: 'center', gap: 16,
              cursor: stat.route ? 'pointer' : 'default',
              transition: 'transform 0.2s, box-shadow 0.2s',
            }}
            onMouseEnter={e => { if (stat.route) { (e.currentTarget as HTMLDivElement).style.transform = 'translateY(-3px)'; (e.currentTarget as HTMLDivElement).style.boxShadow = '0 8px 24px rgba(67,24,255,0.14)'; }}}
            onMouseLeave={e => { (e.currentTarget as HTMLDivElement).style.transform = ''; (e.currentTarget as HTMLDivElement).style.boxShadow = '0 2px 12px rgba(67,24,255,0.07)'; }}
          >
            <div style={{
              width: 48, height: 48, borderRadius: 12,
              background: stat.bg, color: stat.color,
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              {stat.icon}
            </div>
            <div>
              <div style={{ fontSize: 26, fontWeight: 800, color: '#1B254B', lineHeight: 1 }}>
                {loading ? '—' : (stat.value ?? 0)}
              </div>
              <div style={{ fontSize: 13, color: '#A3AED0', marginTop: 2 }}>{stat.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Quick Actions */}
      <h6 style={{ fontWeight: 700, color: '#1B254B', marginBottom: 12, fontSize: 16 }}>Quick Actions</h6>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 14, marginBottom: 28 }}>
        {quickActions.map(action => (
          <div
            key={action.route}
            onClick={() => navigate(action.route)}
            style={{
              background: 'white', borderRadius: 14,
              padding: '18px 20px', boxShadow: '0 2px 10px rgba(67,24,255,0.06)',
              cursor: 'pointer', display: 'flex', alignItems: 'flex-start', gap: 14,
              transition: 'transform 0.18s, box-shadow 0.18s',
            }}
            onMouseEnter={e => { (e.currentTarget as HTMLDivElement).style.transform = 'translateY(-3px)'; (e.currentTarget as HTMLDivElement).style.boxShadow = '0 8px 22px rgba(67,24,255,0.13)'; }}
            onMouseLeave={e => { (e.currentTarget as HTMLDivElement).style.transform = ''; (e.currentTarget as HTMLDivElement).style.boxShadow = '0 2px 10px rgba(67,24,255,0.06)'; }}
          >
            <div style={{
              width: 40, height: 40, borderRadius: 10, flexShrink: 0,
              background: action.color + '18', color: action.color,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              {action.icon}
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: 13, color: '#1B254B', marginBottom: 2 }}>{action.label}</div>
              <div style={{ fontSize: 11, color: '#A3AED0', lineHeight: 1.4 }}>{action.desc}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Recent Proposals */}
      <div style={{
        background: 'white', borderRadius: 16,
        boxShadow: '0 2px 12px rgba(67,24,255,0.07)', overflow: 'hidden',
      }}>
        <div style={{
          padding: '20px 24px', borderBottom: '1px solid #F4F7FE',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <h6 style={{ margin: 0, fontWeight: 700, color: '#1B254B' }}>Recent Proposals</h6>
          <button
            onClick={() => navigate('/admin/proposals/pending')}
            style={{
              background: '#EFF3FF', color: '#4318FF', border: 'none',
              borderRadius: 20, padding: '6px 16px', fontSize: 13, fontWeight: 600, cursor: 'pointer',
            }}
          >
            View All
          </button>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 32, color: '#A3AED0', fontSize: 14 }}>Loading…</div>
        ) : recentProposals.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 32, color: '#A3AED0', fontSize: 14 }}>No proposals found.</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ background: '#F8FAFF' }}>
                  {['ID', 'Title', 'Status', 'Created', 'Action'].map(h => (
                    <th key={h} style={{
                      padding: '12px 20px', textAlign: h === 'Action' ? 'right' : 'left',
                      fontSize: 11, fontWeight: 700, color: '#A3AED0',
                      textTransform: 'uppercase', letterSpacing: 0.8,
                      borderBottom: '1px solid #F4F7FE',
                    }}>{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {recentProposals.map((p, idx) => {
                  const id = p.proposalId ?? p.id;
                  const statusStyle = STATUS_STYLES[p.status ?? ''] ?? { bg: '#F3F4F6', color: '#6B7280' };
                  return (
                    <tr
                      key={id}
                      style={{ borderBottom: idx < recentProposals.length - 1 ? '1px solid #F4F7FE' : 'none' }}
                    >
                      <td style={{ padding: '14px 20px', fontSize: 13, color: '#A3AED0' }}>#{id}</td>
                      <td style={{ padding: '14px 20px', fontWeight: 600, color: '#1B254B', fontSize: 14 }}>{p.title ?? '—'}</td>
                      <td style={{ padding: '14px 20px' }}>
                        <span style={{
                          background: statusStyle.bg, color: statusStyle.color,
                          borderRadius: 20, padding: '3px 12px', fontSize: 11, fontWeight: 700,
                        }}>
                          {p.status ?? '—'}
                        </span>
                      </td>
                      <td style={{ padding: '14px 20px', fontSize: 13, color: '#A3AED0' }}>
                        {p.createdAt ? new Date(p.createdAt).toLocaleDateString() : '—'}
                      </td>
                      <td style={{ padding: '14px 20px', textAlign: 'right' }}>
                        <button
                          onClick={() => navigate(`/admin/proposals/${id}/review`)}
                          style={{
                            background: '#EFF3FF', color: '#4318FF', border: 'none',
                            borderRadius: 20, padding: '6px 16px', fontSize: 12, fontWeight: 600, cursor: 'pointer',
                          }}
                        >
                          Review
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;

