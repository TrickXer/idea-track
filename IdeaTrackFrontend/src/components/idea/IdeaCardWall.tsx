import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { IIdea } from './IIdea';
import { ThumbsUp, ThumbsDown, Eye } from 'lucide-react';

type Props = {
  idea: IIdea;
  onView?: (idea: IIdea) => void;
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

const IdeaCardWall: React.FC<Props> = ({ idea, onView }) => {
  const navigate = useNavigate();

  const date = new Date(idea.createdAt);
  const dateStr = date.toLocaleString(undefined, { month: 'short', day: 'numeric' });

  const catColor = getCategoryColor(idea.category?.name);

  const tags = idea.tag
    ? idea.tag.split(',').map(t => t.trim()).filter(Boolean)
    : [];

  return (
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
        {/* Category badge */}
        <div style={{ marginBottom: 12 }}>
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
        <div style={{ fontSize: 12, color: '#A3AED0', marginBottom: 10 }}>
          by <strong style={{ color: '#4A5568' }}>{idea.author?.displayName ?? 'Unknown'}</strong> · {dateStr}
        </div>

        {/* Tags */}
        {tags.length > 0 && (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 10 }}>
            {tags.map((tag, idx) => (
              <span key={idx} style={{
                background: '#F4F7FE', color: '#4318FF',
                borderRadius: 20, padding: '2px 10px',
                fontSize: 11, fontWeight: 500,
              }}>
                #{tag}
              </span>
            ))}
          </div>
        )}

        {/* Footer */}
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          marginTop: 'auto', paddingTop: 14,
          borderTop: '1px solid #F4F7FE',
        }}>
          <div style={{ display: 'flex', gap: 12 }}>
            <span style={{
              display: 'flex', alignItems: 'center', gap: 4,
              fontSize: 13, color: '#D97706', fontWeight: 600,
            }}>
              <ThumbsUp size={13} /> {idea.votes?.upvotes ?? 0}
            </span>
            <span style={{
              display: 'flex', alignItems: 'center', gap: 4,
              fontSize: 13, color: '#A3AED0', fontWeight: 500,
            }}>
              <ThumbsDown size={13} /> {idea.votes?.downvotes ?? 0}
            </span>
          </div>

          <button
            onClick={() => {
              onView?.(idea);
              navigate(`/idea/${idea.ideaId}`);
            }}
            style={{
              background: '#EFF3FF', color: '#4318FF', border: 'none',
              borderRadius: 20, padding: '6px 16px', fontSize: 13, fontWeight: 600,
              cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 5,
            }}
          >
            <Eye size={13} /> View
          </button>
        </div>
      </div>
    </div>
  );
};

export default IdeaCardWall;
