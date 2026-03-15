import { useEffect, useState } from 'react';
import { useNavigate } from "react-router-dom";
import type { IIdea } from '../../components/idea/IIdea';
import { getIdeaByLoggedUser } from '../../utils/ideaApi';
import { fetchMyProfile as getProfile } from '../../utils/profileApi';
import { useAuth } from '../../utils/authContext';
import IdeaCard from '../../components/idea/IdeaCard';
import { PlusCircle, Compass, Lightbulb, CheckCircle, Clock, FileText } from 'lucide-react';

const EmployeeDashboard = () => {
  const { token } = useAuth();
  const [ideas, setIdeas] = useState<IIdea[]>([]);
  const [loading, setLoading] = useState(false);
  const [displayName, setDisplayName] = useState<string>('User');
  const [userId, setUserId] = useState<number | null>(null);
  
  const navigate = useNavigate();

  const loadProfile = async () => {
    try {
      const data = await getProfile();
      if (data?.name) setDisplayName(data.name);
      if (data?.userId) {
        setUserId(data.userId);
        localStorage.setItem('user-profile', JSON.stringify({ userId: data.userId, name: data.name }));
      }
    } catch (e) {
      console.error('loadProfile failed', e);
    }
  };

  const getData = async () => {
    if (userId == null) return;
    try {
      setLoading(true);
      const res = await getIdeaByLoggedUser(userId);
      const raw = res.data;
      if (Array.isArray(raw)) {
        setIdeas(raw);
      } else if (raw && Array.isArray(raw.content)) {
        setIdeas(raw.content);
      } else {
        setIdeas([]);
      }
    } catch (err) {
      console.error('Auth failed or Server Down:', err);
      setIdeas([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      loadProfile();
    }
  }, [token]);

  useEffect(() => {
    if (userId != null) getData();
  }, [userId]);

  // Derived stats
  const totalIdeas = ideas.length;
  const submittedIdeas = ideas.filter(i => i.ideaStatus && i.ideaStatus !== 'DRAFT').length;
  const acceptedIdeas = ideas.filter(i => i.ideaStatus === 'ACCEPTED' || i.ideaStatus === 'APPROVED').length;
  const draftIdeas = ideas.filter(i => i.ideaStatus === 'DRAFT').length;

  return (
    <div style={{ padding: '0 4px' }}>

      {/* Hero Banner */}
      <div style={{
        background: 'linear-gradient(135deg, #4318FF 0%, #868CFF 100%)',
        borderRadius: 20,
        padding: '32px 36px',
        color: 'white',
        marginBottom: 28,
        position: 'relative',
        overflow: 'hidden',
      }}>
        <div style={{
          position: 'absolute', right: -40, top: -40,
          width: 220, height: 220, borderRadius: '50%',
          background: 'rgba(255,255,255,0.07)'
        }} />
        <div style={{
          position: 'absolute', right: 80, bottom: -60,
          width: 160, height: 160, borderRadius: '50%',
          background: 'rgba(255,255,255,0.05)'
        }} />
        <div style={{ position: 'relative', zIndex: 1 }}>
          <p style={{ margin: 0, opacity: 0.8, fontSize: 14, marginBottom: 4 }}>Good day</p>
          <h3 style={{ margin: 0, fontWeight: 800, fontSize: 26, marginBottom: 8 }}>
            Welcome back, {displayName}!
          </h3>
          <p style={{ margin: 0, opacity: 0.8, fontSize: 14, maxWidth: 460 }}>
            Turn your ideas into impact. Submit proposals, track progress, and collaborate with your team.
          </p>
          <div style={{ display: 'flex', gap: 12, marginTop: 24, flexWrap: 'wrap' }}>
            <button
              style={{
                background: 'white', color: '#4318FF',
                border: 'none', borderRadius: 30,
                padding: '10px 24px', fontWeight: 700,
                fontSize: 14, cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 8,
                transition: '0.2s',
              }}
              onClick={() => navigate('/create-idea')}
            >
              <PlusCircle size={16} /> Post New Idea
            </button>
            <button
              style={{
                background: 'rgba(255,255,255,0.15)',
                color: 'white',
                border: '1.5px solid rgba(255,255,255,0.4)',
                borderRadius: 30,
                padding: '10px 24px', fontWeight: 600,
                fontSize: 14, cursor: 'pointer',
                display: 'flex', alignItems: 'center', gap: 8,
              }}
              onClick={() => navigate('/explore')}
            >
              <Compass size={16} /> Explore Ideas
            </button>
          </div>
        </div>
      </div>

      {/* Stat Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 16, marginBottom: 32 }}>
        {[
          { label: 'Total Ideas', value: totalIdeas, icon: <Lightbulb size={22} />, color: '#4318FF', bg: '#EFF3FF' },
          { label: 'Submitted', value: submittedIdeas, icon: <FileText size={22} />, color: '#38B2AC', bg: '#E6FFFA' },
          { label: 'Accepted', value: acceptedIdeas, icon: <CheckCircle size={22} />, color: '#48BB78', bg: '#F0FFF4' },
          { label: 'Drafts', value: draftIdeas, icon: <Clock size={22} />, color: '#ED8936', bg: '#FFFAF0' },
        ].map(stat => (
          <div key={stat.label} style={{
            background: 'white',
            borderRadius: 16,
            padding: '20px 24px',
            boxShadow: '0 2px 12px rgba(67,24,255,0.07)',
            display: 'flex',
            alignItems: 'center',
            gap: 16,
          }}>
            <div style={{
              width: 48, height: 48, borderRadius: 12,
              background: stat.bg, color: stat.color,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              flexShrink: 0,
            }}>
              {stat.icon}
            </div>
            <div>
              <div style={{ fontSize: 26, fontWeight: 800, color: '#1B254B', lineHeight: 1 }}>{loading ? '—' : stat.value}</div>
              <div style={{ fontSize: 13, color: '#A3AED0', marginTop: 2 }}>{stat.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Ideas Grid */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <h5 style={{ margin: 0, fontWeight: 700, color: '#1B254B', fontSize: 18 }}>My Recent Ideas</h5>
        {ideas.length > 0 && (
          <span style={{ fontSize: 13, color: '#A3AED0' }}>{totalIdeas} idea{totalIdeas !== 1 ? 's' : ''}</span>
        )}
      </div>

      <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        {loading && [1, 2, 3].map(i => (
          <div className="col" key={i}>
            <div className="card rounded-4 border-0 shadow-sm h-100 p-3">
              <div className="card-body">
                <span className="placeholder col-4 rounded-pill mb-3"></span>
                <h5 className="placeholder col-10"></h5>
                <div className="placeholder col-6 mb-4"></div>
                <div className="mt-auto d-flex justify-content-between">
                  <span className="placeholder col-3 rounded-pill"></span>
                  <span className="placeholder col-3 rounded-pill"></span>
                </div>
              </div>
            </div>
          </div>
        ))}

        {!loading && ideas.length === 0 && (
          <div className="col-12">
            <div style={{
              background: 'white',
              borderRadius: 20,
              padding: '48px 24px',
              textAlign: 'center',
              boxShadow: '0 2px 12px rgba(67,24,255,0.07)',
            }}>
              <div style={{ marginBottom: 12, color: '#4318FF' }}><Lightbulb size={48} /></div>
              <h5 style={{ color: '#1B254B', fontWeight: 700, marginBottom: 6 }}>No ideas yet</h5>
              <p style={{ color: '#A3AED0', fontSize: 14, marginBottom: 20 }}>
                You haven't posted any ideas yet. Share your first idea and start making an impact!
              </p>
              <button
                onClick={() => navigate('/create-idea')}
                style={{
                  background: 'linear-gradient(135deg, #4318FF, #868CFF)',
                  color: 'white', border: 'none', borderRadius: 30,
                  padding: '10px 28px', fontWeight: 600, fontSize: 14, cursor: 'pointer',
                }}
              >
                Post My First Idea
              </button>
            </div>
          </div>
        )}

        {!loading && ideas.map((idea) => (
          <div className="col" key={idea.ideaId}>
            <IdeaCard 
              idea={idea} 
              onDeleteSuccess={(deletedId) => {
                setIdeas(prev => prev.filter(i => i.ideaId !== deletedId));
              }}
            />
          </div>
        ))}
      </div>
    </div>
  );
};

export default EmployeeDashboard;