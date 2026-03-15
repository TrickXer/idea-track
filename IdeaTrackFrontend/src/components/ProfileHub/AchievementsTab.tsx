import styles from './AchievementsTab.module.css';
import { Medal } from 'lucide-react';

interface AchievementsTabProps {
  badges: string[];
}

export const AchievementsTab = ({ badges }:AchievementsTabProps) => (
  <div className="row g-4">
    {badges.map((badge, i) => (
      <div key={i} className="col-6 col-md-4 col-lg-3 col-xl-2">
        <div
          className={`card p-5 rounded-3 text-center h-100 d-flex flex-column align-items-center justify-content-center ${styles.badgeCard}`}
        >
          <div className={styles.badgeEmoji}><Medal size={32} /></div>
          <small className={`fw-bold text-uppercase text-secondary ${styles.badgeText}`}>
            {badge}
          </small>
        </div>
      </div>
    ))}
  </div>
);
