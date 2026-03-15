import { useEffect, useState } from 'react';
import AcceptedIdeasList from '../../components/proposals/AcceptedIdeasList';
import { fetchMyProfile } from '../../utils/profileApi';

export default function AcceptedIdeasPage() {
  const [userId, setUserId] = useState<number | null>(null);

  useEffect(() => {
    async function loadUser() {
      try {
        const profile = await fetchMyProfile();
        setUserId(profile.userId);
      } catch (e) {
        console.error("Failed to load logged-in user:", e);
      }
    }
    loadUser();
  }, []);

  if (userId === null) return <div>Loading…</div>;

  return (
    <div className="container my-4">
      <AcceptedIdeasList userId={userId} />
    </div>
  );
}