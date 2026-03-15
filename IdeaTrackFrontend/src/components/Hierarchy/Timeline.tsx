// src/components/Hierarchy/Timeline.tsx
import { Calendar, Clock } from 'lucide-react';
import type { TimelineEntry } from './HierarchyTypes';
import styles from './Timeline.module.css';

interface TimelineProps {
  events: TimelineEntry[];
  offsetTop?: number;
}

export const Timeline = ({ events, offsetTop = 120 }:TimelineProps) => (
  <div
    className={`card p-4 p-sm-5 ${styles.timelineCard}`}
    style={{
      top: `${offsetTop}px`
    }}
    aria-label="Idea audit timeline"
  >
    {/* Header */}
    <div className="d-flex align-items-center gap-3 mb-4">
      <div className={`rounded d-flex align-items-center justify-content-center bg-info bg-opacity-10 ${styles.headerIcon}`}>
        <Calendar size={18} className="text-info" />
      </div>
      <h5 className={`small fw-bold text-uppercase ${styles.headerTitle}`}>
        Audit Timeline
      </h5>
    </div>

    {/* Rail + items */}
    <div className="position-relative">
      {/* vertical rail */}
      <div
        className={styles.rail}
        aria-hidden="true"
      />

      {events.map((event, i) => (
        <div key={i} className={`position-relative ${styles.timelineItem}`}>
          {/* node dot */}
          <div
            className={`position-absolute d-flex align-items-center justify-content-center ${styles.nodeDot}`}
            aria-hidden="true"
          >
            <div
              className={`rounded-circle bg-info ${styles.nodeDotCircle}`}
              style={{
                boxShadow: '0 0 0 6px rgba(13, 110, 253, 0.1), 0 0 18px rgba(13, 110, 253, 0.4)'
              }}
            />
          </div>

          {/* card */}
          <div className="card p-3 p-sm-3 small">
            <p className="mb-2 fw-bold">
              {event.title}
            </p>
            <div className="d-flex align-items-center gap-2 text-info" style={{ opacity: 0.9 }}>
              <Clock size={12} />
              <span
                className={`text-uppercase fw-bold ${styles.timelineItemText}`}
                aria-label="Event timestamp"
              >
                {new Date(event.date).toLocaleString([], {
                  month: 'short',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit'
                })}
              </span>
            </div>
          </div>
        </div>
      ))}
    </div>
  </div>
);