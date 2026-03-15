import { useState, useEffect } from 'react';
import { User, CheckCircle2, Target, Save, AlertCircle } from 'lucide-react';
import { useShowToast } from '../../hooks/useShowToast';
import { successMessages } from '../../utils/axiosInterceptor';
import { updateProfile } from '../../utils/profileHierarchy';
import type { UserProfileDTO } from './ProfileTypes';
import styles from './OverviewTab.module.css';

interface OverviewTabProps {
  user: UserProfileDTO;
  isEditing: boolean;
  onProfileUpdated?: () => void | Promise<void>;
  onEditingChange?: (isEditing: boolean) => void;
}

// Field validation rules
const validationRules = {
  name: {
    required: true,
    minLength: 2,
    maxLength: 100,
    pattern: /^[a-zA-Z\s'-]+$/,
    message: 'Name must be 2-100 characters and contain only letters, spaces, hyphens, and apostrophes',
  },
  phone: {
    required: true,
    // supports digits, +, -, (, ), spaces; length 7-20 (loose client-side gate)
    pattern: /^[+()\-\s\d]{7,20}$/,
    message: 'Phone number must be 7-20 characters and contain only digits, +, -, (, ), or spaces',
  },
  bio: {
    maxLength: 500,
    message: 'Bio must not exceed 500 characters',
  },
};

type ValidationErrors = {
  name?: string;
  phone?: string;
  bio?: string;
};

// Editable field (controlled) – see snippet above
const EditableField = ({
  label,
  value,
  isEditing,
  textarea,
  error,
  onChange,
  onBlur,
}: {
  label: string;
  value: string | number | null | undefined;
  isEditing: boolean;
  textarea?: boolean;
  error?: string;
  onChange?: (value: string) => void;
  onBlur?: () => void;
}) => (
  <div className="mb-4">
    <label className={`form-label small fw-bold text-uppercase d-block text-secondary ${styles.fieldLabel}`}>
      {label}
    </label>

    {isEditing ? (
      <>
        {textarea ? (
          <textarea
            className={`form-control p-3 rounded-3 ${error ? 'is-invalid' : ''} ${styles.fieldInput}`}
            value={value ?? ''}
            onChange={(e) => onChange?.(e.target.value)}
            onBlur={onBlur}
          />
        ) : (
          <input
            type="text"
            className={`form-control p-3 rounded-3 ${error ? 'is-invalid' : ''} ${styles.fieldInput}`}
            value={value ?? ''}
            onChange={(e) => onChange?.(e.target.value)}
            onBlur={onBlur}
          />
        )}
        {error && (
          <div className={`d-flex align-items-center gap-2 mt-2 text-danger small ${styles.errorText}`}>
            <AlertCircle size={14} />
            {error}
          </div>
        )}
      </>
    ) : (
      <div className={`p-3 rounded-3 fw-semibold text-secondary ${styles.fieldDisplay}`}>
        {value ? String(value) : 'No information provided'}
      </div>
    )}
  </div>
);

// Read-only field (unchanged)
const ReadOnlyField = ({ label, value }: { label: string; value: string | number | null | undefined }) => (
  <div className="mb-4">
    <label className={`form-label small fw-bold text-uppercase d-block text-secondary ${styles.fieldLabel}`}>
      {label}
    </label>
    <div className={`p-3 rounded-3 fw-semibold text-secondary ${styles.fieldDisplay}`}>
      {value ? String(value) : 'No information provided'}
    </div>
  </div>
);

export const OverviewTab = ({ user, isEditing, onProfileUpdated, onEditingChange }: OverviewTabProps) => {
  const toast = useShowToast();

  // Progress math for level bar
  const totalForNext = (user.totalXP ?? 0) + (user.xpToNext ?? 0);
  const xpProgress = totalForNext > 0 ? Math.min(100, Math.round((user.totalXP / totalForNext) * 100)) : 0;

  const [isSaving, setIsSaving] = useState(false);

  // Controlled form state
  const [name, setName] = useState(user.name ?? '');
  const [phone, setPhone] = useState(user.phoneNo ?? '');
  const [bio, setBio] = useState(user.bio ?? '');

  // Errors
  const [errors, setErrors] = useState<ValidationErrors>({});

  // If parent toggles editing off/on or user object changes, initialize fields
  useEffect(() => {
    if (!isEditing) {
      // Reset values to latest server state when exiting edit mode
      setName(user.name ?? '');
      setPhone(user.phoneNo ?? '');
      setBio(user.bio ?? '');
      setErrors({});
    } else {
      // When entering edit mode, seed from current user values
      setName((prev) => (prev === '' ? user.name ?? '' : prev));
      setPhone((prev) => (prev === '' ? user.phoneNo ?? '' : prev));
      setBio((prev) => (prev === '' ? user.bio ?? '' : prev));
    }
  }, [isEditing, user.name, user.phoneNo, user.bio]);

  // --- Validators (same rules as before) ---
  const validateName = (val: string): string | undefined => {
    const v = val.trim();
    if (validationRules.name.required && !v) return 'Name is required';
    if (v.length < validationRules.name.minLength) return `Name must be at least ${validationRules.name.minLength} characters`;
    if (v.length > validationRules.name.maxLength) return `Name must not exceed ${validationRules.name.maxLength} characters`;
    if (!validationRules.name.pattern.test(v)) return validationRules.name.message;
    return undefined;
  };

  const validatePhone = (val: string): string | undefined => {
    const v = val.trim();
    if (validationRules.phone.required && !v) return 'Phone number is required';
    if (!validationRules.phone.pattern.test(v)) return validationRules.phone.message;
    return undefined;
  };

  const validateBio = (val: string): string | undefined => {
    const v = val.trim();
    if (v && v.length > validationRules.bio.maxLength) {
      return `Bio must not exceed ${validationRules.bio.maxLength} characters`;
    }
    return undefined;
  };

  const validateAll = (): ValidationErrors => {
    const newErrors: ValidationErrors = {};
    const eName = validateName(name);
    const ePhone = validatePhone(phone);
    const eBio = validateBio(bio);
    if (eName) newErrors.name = eName;
    if (ePhone) newErrors.phone = ePhone;
    if (eBio) newErrors.bio = eBio;
    return newErrors;
  };

  // --- Live onChange validation (validate just the field that changed) ---
  const handleNameChange = (val: string) => {
    setName(val);
    setErrors((prev) => ({ ...prev, name: validateName(val) }));
  };

  const handlePhoneChange = (val: string) => {
    setPhone(val);
    setErrors((prev) => ({ ...prev, phone: validatePhone(val) }));
  };

  const handleBioChange = (val: string) => {
    setBio(val);
    setErrors((prev) => ({ ...prev, bio: validateBio(val) }));
  };

  // Optional: onBlur ensures error shows only after first interaction
  const handleNameBlur = () => setErrors((p) => ({ ...p, name: validateName(name) }));
  const handlePhoneBlur = () => setErrors((p) => ({ ...p, phone: validatePhone(phone) }));
  const handleBioBlur = () => setErrors((p) => ({ ...p, bio: validateBio(bio) }));

  const handleSaveChanges = async () => {
    const validationErrors = validateAll();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    try {
      setIsSaving(true);
      const updatedData = {
        name: name.trim(),
        phoneNo: phone.trim(),
        bio: bio.trim(), // allow empty bio
      };
      await updateProfile(updatedData);
      setErrors({});
      await onProfileUpdated?.();
      toast.success(successMessages.profileUpdated);
      onEditingChange?.(false);
    } catch (err) {
      console.error('Failed to save profile:', err);
      // Error toast is handled by interceptor
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 28 }}>
      {/* LEFT: Personal Details */}
      <div style={{ gridColumn: 'span 2' }}>
        <div
          style={{
            background: 'white',
            borderRadius: 20,
            padding: 32,
            boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
            border: 'none',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 32 }}>
            <h3
              style={{
                fontSize: 18,
                fontWeight: 700,
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                marginBottom: 0,
                color: '#2B3674',
              }}
            >
              <User size={22} color="#4318FF" />
              Personal Details
            </h3>

            {user.completionPercent === 100 && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                  fontSize: 10,
                  fontWeight: 700,
                  color: '#01B574',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                <CheckCircle2 size={12} />
                PROFILE VERIFIED
              </div>
            )}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24, marginBottom: 24 }}>
            <EditableField
              label="Full Name"
              value={name}
              isEditing={isEditing}
              error={errors.name}
              onChange={handleNameChange}
              onBlur={handleNameBlur}
            />

            <EditableField
              label="Phone"
              value={phone}
              isEditing={isEditing}
              error={errors.phone}
              onChange={handlePhoneChange}
              onBlur={handlePhoneBlur}
            />
          </div>

          <div style={{ marginBottom: 24 }}>
            <ReadOnlyField label="Department" value={user.departmentName} />
          </div>

          <div style={{ marginBottom: 24 }}>
            <EditableField
              label="Bio"
              value={bio}
              isEditing={isEditing}
              textarea
              error={errors.bio}
              onChange={handleBioChange}
              onBlur={handleBioBlur}
            />
          </div>

          {!isEditing && (
            <button
              onClick={() => onEditingChange?.(true)}
              style={{
                background: 'transparent',
                border: '2px solid #4318FF',
                color: '#4318FF',
                padding: '12px 24px',
                borderRadius: 12,
                fontWeight: 700,
                cursor: 'pointer',
                fontSize: 14,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                transition: 'all 0.3s ease',
                marginTop: 24,
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = '#4318FF';
                e.currentTarget.style.color = 'white';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent';
                e.currentTarget.style.color = '#4318FF';
              }}
            >
              Customize Profile
            </button>
          )}

          {isEditing && (
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 24 }}>
              <button
                onClick={handleSaveChanges}
                disabled={isSaving}
                style={{
                  background: '#4318FF',
                  color: 'white',
                  border: 'none',
                  padding: '12px 24px',
                  borderRadius: 12,
                  fontWeight: 700,
                  cursor: isSaving ? 'not-allowed' : 'pointer',
                  fontSize: 14,
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  opacity: isSaving ? 0.6 : 1,
                  transition: 'all 0.3s ease',
                }}
                onMouseEnter={(e) => !isSaving && (e.currentTarget.style.boxShadow = '0px 6px 16px rgba(67, 24, 255, 0.3)')}
                onMouseLeave={(e) => (e.currentTarget.style.boxShadow = 'none')}
              >
                <Save size={18} /> {isSaving ? 'Saving...' : 'Update Profile'}
              </button>
              <button
                onClick={() => onEditingChange?.(false)}
                disabled={isSaving}
                style={{
                  background: 'white',
                  border: '1px solid #E9EDF7',
                  color: '#2B3674',
                  padding: '12px 24px',
                  borderRadius: 12,
                  fontWeight: 700,
                  cursor: isSaving ? 'not-allowed' : 'pointer',
                  fontSize: 14,
                  transition: 'all 0.3s ease',
                  opacity: isSaving ? 0.6 : 1,
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = '#F4F7FE';
                  e.currentTarget.style.borderColor = '#A3AED0';
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = 'white';
                  e.currentTarget.style.borderColor = '#E9EDF7';
                }}
              >
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>

      {/* RIGHT: Completion + Level */}
      <div>
        {/* Completion card */}
        <div
          style={{
            background: 'white',
            borderRadius: 20,
            padding: 32,
            boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
            border: 'none',
            marginBottom: 28,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h5
              style={{
                fontSize: 10,
                fontWeight: 700,
                textTransform: 'uppercase',
                letterSpacing: '0.5px',
                marginBottom: 0,
                display: 'flex',
                alignItems: 'center',
                gap: 8,
                color: '#4318FF',
              }}
            >
              <Target size={14} />
              Completion
            </h5>
            <span style={{ fontSize: 18, fontWeight: 700, color: '#2B3674' }}>{user.completionPercent}%</span>
          </div>

          <div
            style={{
              width: '100%',
              height: 12,
              background: '#E9EDF7',
              borderRadius: 6,
              overflow: 'hidden',
              marginBottom: 12,
            }}
          >
            <div
              style={{
                height: '100%',
                width: `${user.completionPercent}%`,
                background: 'linear-gradient(135deg, #4318FF 0%, #6C63FF 100%)',
                transition: 'width 0.3s ease',
              }}
            />
          </div>

          <small style={{ fontSize: 12, color: '#A3AED0' }}>
            {user.completionPercent === 100 ? 'Your profile is complete!' : 'Complete your bio and avatar to reach 100%'}
          </small>
        </div>

        {/* Level progression card */}
        <div
          style={{
            background: 'white',
            borderRadius: 20,
            padding: 32,
            boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
            border: 'none',
          }}
        >
          <h5 style={{ fontSize: 10, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: 24, color: '#2B3674' }}>
            Level Progression
          </h5>

          <div style={{ width: '100%', height: 12, background: '#E9EDF7', borderRadius: 6, overflow: 'hidden', marginBottom: 24 }}>
            <div
              style={{
                height: '100%',
                width: `${xpProgress}%`,
                background: 'linear-gradient(135deg, #4318FF 0%, #6C63FF 100%)',
                transition: 'width 0.3s ease',
              }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <div>
              <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 4, color: '#2B3674' }}>{user.totalXP} XP</div>
              <small style={{ fontSize: 10, color: '#A3AED0', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Current Score</small>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 14, fontWeight: 700, marginBottom: 4, color: '#4318FF' }}>
                {user.xpToNext === 0 ? 'Max Level' : `+${user.xpToNext} XP`}
              </div>
              <small style={{ fontSize: 10, color: '#A3AED0', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                {user.xpToNext === 0 ? 'Reached' : 'To Next Level'}
              </small>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};