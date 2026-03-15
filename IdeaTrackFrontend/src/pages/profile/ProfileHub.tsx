
import { useState, useEffect, useRef } from 'react';
import { Award, User, Shield, Camera, History, Trash2 } from 'lucide-react';
import { useShowToast } from '../../hooks/useShowToast';
import { successMessages } from '../../utils/axiosInterceptor';
import { ProfileAvatar } from '../../utils/profileImageHandler';
import ConfirmationModal from '../../components/ConfirmationModal/ConfirmationModal';
import { OverviewTab } from '../../components/ProfileHub/OverviewTab';
import { ActivityTab } from '../../components/ProfileHub/ActivityTab';
import { AchievementsTab } from '../../components/ProfileHub/AchievementsTab';
import { SecurityTab } from '../../components/ProfileHub/SecurityTab';
import { getMyProfile, getXPHistory, uploadProfilePhoto, deleteProfilePhoto } from '../../utils/profileHierarchy';
import type { UserProfileDTO, UserActivityDTO } from '../../components/ProfileHub/ProfileTypes';
import { mapApiToUIProfile } from '../../components/ProfileHub/ProfileMapper';

const ProfileHub = () => {
  const toast = useShowToast();
  const [activeTab, setActiveTab] = useState('overview');
  const [user, setUser] = useState<UserProfileDTO | null>(null);
  const [activities, setActivities] = useState<UserActivityDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const refetchProfile = async () => {
    const p = await getMyProfile();
    setUser(mapApiToUIProfile(p)); // ✅ normalize here too
  };

  // Callback for when profile is successfully updated
  const handleProfileUpdated = async () => {
    await refetchProfile();
    setIsEditing(false); // Exit editing mode after successful update
  };

  // Callback for when password is successfully changed
  const handlePasswordChanged = async () => {
    await refetchProfile();
    // Password changed, profile might be updated, optionally exit security tab
  };

  useEffect(() => {
    const fetchUserData = async () => {
      try {
        setLoading(true);
        setError(null);

        const profileData = await getMyProfile();
        setUser(mapApiToUIProfile(profileData)); // ✅ normalize API → UI

        const activitiesData = await getXPHistory();
        setActivities(Array.isArray(activitiesData) ? activitiesData : []);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load profile');
        console.error('Error loading profile:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchUserData();
  }, []);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setUploading(true);
      await uploadProfilePhoto(file);
      await refetchProfile(); // ✅ keep refetch to remain source-of-truth
      toast.success(successMessages.photoUploaded);
    } catch (err) {
      console.error('Failed to upload profile photo:', err);
      // Error toast shown automatically by interceptor
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDeleteProfilePhoto = async () => {
    setShowDeleteConfirmation(true);
  };

  const handleConfirmDelete = async () => {
    try {
      setUploading(true);
      await deleteProfilePhoto();
      await refetchProfile(); // ✅ refresh to get updated profile without photo
      toast.success(successMessages.photoDeleted);
      setShowDeleteConfirmation(false);
    } catch (err) {
      console.error('Failed to delete profile photo:', err);
      // Error toast shown automatically by interceptor
    } finally {
      setUploading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center">
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading your profile...</p>
        </div>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center">
        <div className="alert alert-danger" role="alert">
          <strong>Error:</strong> {error || 'Failed to load profile. Please try again.'}
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: '0 4px' }}>
      {/* CONFIRMATION MODAL FOR DELETE */}
      <ConfirmationModal
        isOpen={showDeleteConfirmation}
        title="Delete Profile Picture"
        message="Are you sure you want to delete your profile picture? This action cannot be undone."
        confirmText="Delete"
        cancelText="Cancel"
        isDangerous={true}
        isLoading={uploading}
        onConfirm={handleConfirmDelete}
        onCancel={() => setShowDeleteConfirmation(false)}
        icon={<Trash2 size={40} style={{ color: '#ef4444' }} />}
      />

      {/* HERO BANNER WITH PROFILE HEADER */}
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
          <div style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
            {/* Avatar Section */}
            <div style={{ position: 'relative', flex: 'shrink-0' }}>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleImageUpload}
                style={{ display: 'none' }}
              />

              <ProfileAvatar
                profileUrl={user.profileUrl}
                userName={user.name}
                size={120}
                className="rounded-circle"
              />

              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  border: '4px solid rgba(255, 255, 255, 0.3)',
                  boxShadow: '0px 10px 30px rgba(0,0,0,0.2)',
                  borderRadius: '50%',
                  pointerEvents: 'none'
                }}
              />

              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={uploading}
                style={{
                  position: 'absolute',
                  bottom: '-15px',
                  right: '-15px',
                  width: '44px',
                  height: '44px',
                  borderRadius: '50%',
                  background: 'white',
                  border: 'none',
                  cursor: uploading ? 'not-allowed' : 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  opacity: uploading ? 0.6 : 1,
                  boxShadow: '0px 4px 12px rgba(0,0,0,0.15)',
                  color: '#4318FF',
                  transition: 'all 0.3s ease'
                }}
                title="Upload profile picture"
              >
                <Camera size={20} />
              </button>

              {user.profileUrl && (
                <button
                  onClick={handleDeleteProfilePhoto}
                  disabled={uploading}
                  style={{
                    position: 'absolute',
                    top: '-15px',
                    right: '-15px',
                    width: '44px',
                    height: '44px',
                    borderRadius: '50%',
                    background: '#EE5D50',
                    border: 'none',
                    cursor: uploading ? 'not-allowed' : 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    opacity: uploading ? 0.6 : 1,
                    boxShadow: '0px 4px 12px rgba(0,0,0,0.15)',
                    color: 'white',
                    transition: 'all 0.3s ease'
                  }}
                  title="Delete profile picture"
                >
                  <Trash2 size={20} />
                </button>
              )}
            </div>

            {/* Profile Info and Stats Section */}
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12, flexWrap: 'wrap' }}>
                <h1 style={{ margin: 0, fontSize: 32, fontWeight: 800 }}>{user.name}</h1>
                <span style={{
                  background: 'rgba(255, 255, 255, 0.25)',
                  backdropFilter: 'blur(10px)',
                  color: 'white',
                  padding: '6px 14px',
                  borderRadius: 20,
                  fontSize: 12,
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px'
                }}>
                  {user.role}
                </span>
              </div>
              <p style={{ margin: '0 0 16px 0', opacity: 0.9, fontSize: 14 }}>
                {user.email} • {user.phoneNo}
              </p>
            </div>

            {/* Quick Stats - Right side, vertically centered */}
            <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', alignItems: 'center', justifyContent: 'flex-end' }}>
              <div style={{ textAlign: 'center' }}>
                <p style={{ margin: '0 0 4px 0', fontSize: 12, opacity: 0.8, textTransform: 'uppercase', letterSpacing: '0.5px', fontWeight: 600 }}>Tier</p>
                <p style={{ margin: 0, fontSize: 24, fontWeight: 800 }}>{user.level}</p>
              </div>
              <div style={{ width: '1px', height: '60px', background: 'rgba(255, 255, 255, 0.3)' }} />
              <div style={{ textAlign: 'center' }}>
                <p style={{ margin: '0 0 4px 0', fontSize: 12, opacity: 0.8, textTransform: 'uppercase', letterSpacing: '0.5px', fontWeight: 600 }}>Total XP</p>
                <p style={{ margin: 0, fontSize: 24, fontWeight: 800 }}>{user.totalXP}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* TABS SECTION */}
      <div style={{ marginBottom: 28 }}>
        <div style={{
          display: 'flex',
          gap: 24,
          borderBottom: '1px solid #E9EDF7',
          overflowX: 'auto',
          paddingBottom: 0
        }}>
          {[
            { id: 'overview', label: 'Overview', icon: <User size={16} /> },
            { id: 'activity', label: 'Activity & XP', icon: <History size={16} /> },
            { id: 'achievements', label: 'Badges', icon: <Award size={16} /> },
            { id: 'security', label: 'Security', icon: <Shield size={16} /> }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              style={{
                padding: '16px 0',
                border: 'none',
                background: 'transparent',
                cursor: 'pointer',
                color: activeTab === tab.id ? '#4318FF' : '#A3AED0',
                fontSize: 14,
                fontWeight: activeTab === tab.id ? 700 : 500,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                borderBottom: activeTab === tab.id ? '2px solid #4318FF' : 'transparent',
                transition: 'all 0.3s ease',
                whiteSpace: 'nowrap',
                position: 'relative',
                bottom: '-1px'
              }}
            >
              {tab.icon}
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* TAB CONTENT */}
      <div>
        {activeTab === 'overview' && <OverviewTab user={user} isEditing={isEditing} onProfileUpdated={handleProfileUpdated} onEditingChange={setIsEditing} />}
        {activeTab === 'activity' && <ActivityTab initialActivities={activities} />}
        {activeTab === 'achievements' && <AchievementsTab badges={user.badges || []} />}
        {activeTab === 'security' && <SecurityTab onPasswordChanged={handlePasswordChanged} />}
      </div>
    </div>
  );
};

export default ProfileHub;
