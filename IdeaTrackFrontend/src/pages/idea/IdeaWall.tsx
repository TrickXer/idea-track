import React, { useEffect, useState } from 'react';
import { getAllIdeas as GetAllIdeas } from '../../utils/ideaApi';
import type { IIdea } from '../../components/idea/IIdea';
import IdeaCardWall from '../../components/idea/IdeaCardWall';
import { Search, Sparkles } from 'lucide-react';

const IdeaWall: React.FC = () => {
  const [ideas, setIdeas] = useState<IIdea[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');

  const [currentPage, setCurrentPage] = useState<number>(0);
  const [isLastPage, setIsLastPage] = useState<boolean>(false);
  const [isFetchingMore, setIsFetchingMore] = useState<boolean>(false);

  useEffect(() => {
    fetchIdeas(0, '', false);
  }, []);

  const fetchIdeas = async (pageNum: number, queryText: string, append: boolean) => {
    if (append) setIsFetchingMore(true);
    else setLoading(true);

    try {
      const searchPart = queryText ? `q=${encodeURIComponent(queryText)}&` : '';
      const queryString = `?${searchPart}page=${pageNum}&size=10`;

      const response = await GetAllIdeas(queryString);
      const newIdeas = response.data.content || [];

      setIdeas(prev => append ? [...prev, ...newIdeas] : newIdeas);
      setIsLastPage(response.data.last || newIdeas.length < 10);
      setCurrentPage(pageNum);
    } catch (error) {
      console.error('Error fetching ideas:', error);
    } finally {
      setLoading(false);
      setIsFetchingMore(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    fetchIdeas(0, searchQuery, false);
  };

  const handleLoadMore = () => {
    fetchIdeas(currentPage + 1, searchQuery, true);
  };

  return (
    <div style={{ padding: '0 4px' }}>

      {/* Hero Header */}
      <div style={{
        background: 'linear-gradient(135deg, #4318FF 0%, #868CFF 100%)',
        borderRadius: 20,
        padding: '36px',
        color: 'white',
        marginBottom: 32,
        position: 'relative',
        overflow: 'hidden',
        textAlign: 'center',
      }}>
        <div style={{ position: 'absolute', left: -60, top: -60, width: 200, height: 200, borderRadius: '50%', background: 'rgba(255,255,255,0.06)' }} />
        <div style={{ position: 'absolute', right: -40, bottom: -50, width: 180, height: 180, borderRadius: '50%', background: 'rgba(255,255,255,0.05)' }} />
        <div style={{ position: 'relative', zIndex: 1 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, marginBottom: 8 }}>
            <Sparkles size={20} />
            <span style={{ fontSize: 13, opacity: 0.85, fontWeight: 600, letterSpacing: 1, textTransform: 'uppercase' }}>
              Community Ideas
            </span>
          </div>
          <h2 style={{ margin: 0, fontWeight: 800, fontSize: 28, marginBottom: 8 }}>Explore the Idea Wall</h2>
          <p style={{ margin: 0, opacity: 0.8, fontSize: 14 }}>
            Discover, vote, and be inspired by ideas from across the organisation.
          </p>

          {/* Search bar inline in hero */}
          <form
            onSubmit={handleSearchSubmit}
            style={{
              display: 'flex', gap: 0, marginTop: 24, maxWidth: 520,
              margin: '24px auto 0', borderRadius: 40,
              overflow: 'hidden', boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
            }}
          >
            <div style={{ position: 'relative', flex: 1 }}>
              <Search size={16} style={{
                position: 'absolute', left: 18, top: '50%',
                transform: 'translateY(-50%)', color: '#A3AED0',
              }} />
              <input
                type="text"
                placeholder="Search ideas, tags, authors…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                style={{
                  width: '100%', border: 'none', outline: 'none',
                  padding: '14px 16px 14px 44px',
                  fontSize: 14, background: 'white', color: '#1B254B',
                  boxSizing: 'border-box',
                }}
              />
            </div>
            <button
              type="submit"
              style={{
                background: '#1B254B', color: 'white', border: 'none',
                padding: '0 24px', fontWeight: 700, fontSize: 14, cursor: 'pointer',
                whiteSpace: 'nowrap',
              }}
            >
              Search
            </button>
          </form>
        </div>
      </div>

      {/* Ideas count */}
      {!loading && ideas.length > 0 && (
        <p style={{ color: '#A3AED0', fontSize: 13, marginBottom: 16 }}>
          Showing {ideas.length} idea{ideas.length !== 1 ? 's' : ''}
          {searchQuery && <> matching "<strong style={{ color: '#4318FF' }}>{searchQuery}</strong>"</>}
        </p>
      )}

      {/* GRID */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0' }}>
          <div className="spinner-border" style={{ color: '#4318FF' }}></div>
          <p style={{ color: '#A3AED0', marginTop: 12, fontSize: 14 }}>Loading ideas…</p>
        </div>
      ) : ideas.length === 0 ? (
        <div style={{
          background: 'white', borderRadius: 20, padding: '60px 24px',
          textAlign: 'center', boxShadow: '0 2px 12px rgba(67,24,255,0.07)',
        }}>
          <div style={{ marginBottom: 12, color: '#4318FF' }}><Search size={48} /></div>
          <h5 style={{ color: '#1B254B', fontWeight: 700, marginBottom: 6 }}>No ideas found</h5>
          <p style={{ color: '#A3AED0', fontSize: 14 }}>
            {searchQuery ? `No ideas match "${searchQuery}". Try a different search.` : 'No ideas have been posted yet.'}
          </p>
        </div>
      ) : (
        <>
          <div className="row g-4">
            {ideas.map((item) => (
              <div key={item.ideaId} className="col-12 col-md-6 col-lg-4">
                <IdeaCardWall idea={item} onView={() => {}} />
              </div>
            ))}
          </div>

          {!isLastPage && (
            <div style={{ textAlign: 'center', marginTop: 40 }}>
              <button
                onClick={handleLoadMore}
                disabled={isFetchingMore}
                style={{
                  background: isFetchingMore ? '#E9EDF7' : 'linear-gradient(135deg, #4318FF, #868CFF)',
                  color: isFetchingMore ? '#A3AED0' : 'white',
                  border: 'none', borderRadius: 30,
                  padding: '12px 36px', fontWeight: 700, fontSize: 14,
                  cursor: isFetchingMore ? 'not-allowed' : 'pointer',
                  display: 'inline-flex', alignItems: 'center', gap: 8,
                }}
              >
                {isFetchingMore ? (
                  <>
                    <span className="spinner-border spinner-border-sm"></span>
                    Loading more…
                  </>
                ) : 'Load More Ideas'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default IdeaWall;
