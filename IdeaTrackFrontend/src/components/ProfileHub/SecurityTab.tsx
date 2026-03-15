import { useState, useMemo } from 'react';
import { Trash2, AlertCircle } from 'lucide-react';
import { useShowToast } from '../../hooks/useShowToast';
import { successMessages } from '../../utils/axiosInterceptor';
import { changePassword, deleteProfile } from '../../utils/profileHierarchy';
import ConfirmationModal from '../ConfirmationModal/ConfirmationModal';

// Password validation rules
const passwordValidationRules = {
  minLength: 8,
  requiresUppercase: true,
  requiresLowercase: true,
  requiresDigit: true,
  requiresSpecial: true,
  // NOTE: keep this a plain string; the & will be treated literally
  specialChars: '@#$%^&+=!'
};

type PasswordErrors = {
  currentPassword?: string;
  newPassword?: string;
};

const PasswordField = ({
  label,
  value,
  error,
  onChange,
  onBlur
}: {
  label: string;
  value: string;
  error?: string;
  onChange?: (value: string) => void;
  onBlur?: () => void;
}) => (
  <div className="mb-4">
    <label
      className="form-label small fw-bold text-uppercase d-block text-secondary"
      style={{ fontSize: '10px', letterSpacing: '0.05em', marginBottom: '0.75rem' }}
    >
      {label}
    </label>
    <input
      value={value}
      onChange={(e) => onChange?.(e.target.value)}
      onBlur={onBlur}
      type="password"
      placeholder="••••••••••••"
      className={`form-control p-3 rounded-3 ${error ? 'is-invalid' : ''}`}
      style={{ fontSize: '14px' }}
    />
    {error && (
      <div className="d-flex align-items-center gap-2 mt-2 text-danger small">
        <AlertCircle size={14} />
        {error}
      </div>
    )}
  </div>
);

interface SecurityTabProps {
  onPasswordChanged?: () => void | Promise<void>;
}

export const SecurityTab = ({ onPasswordChanged }: SecurityTabProps) => {
  const toast = useShowToast();
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [isDeletingProfile, setIsDeletingProfile] = useState(false);
  const [showDeleteConfirmation, setShowDeleteConfirmation] = useState(false);

  // form state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');

  // errors
  const [errors, setErrors] = useState<PasswordErrors>({});

  // Precompute a safe character class for special chars (escape regex meta)
  const escapedSpecials = useMemo(() => {
    // escape: \ ^ $ * + ? . ( ) | { } [ ]
    const escapeRegex = /[\\^$*+?.()|[\]{}]/g;
    return passwordValidationRules.specialChars.replace(escapeRegex, '\\$&');
  }, []);

  const validatePassword = (password: string, fieldName: string): string | undefined => {
    if (!password) {
      return `${fieldName} is required`;
    }
    if (password.length < passwordValidationRules.minLength) {
      return `${fieldName} must be at least ${passwordValidationRules.minLength} characters`;
    }
    if (passwordValidationRules.requiresUppercase && !/[A-Z]/.test(password)) {
      return `${fieldName} must contain at least one uppercase letter`;
    }
    if (passwordValidationRules.requiresLowercase && !/[a-z]/.test(password)) {
      return `${fieldName} must contain at least one lowercase letter`;
    }
    if (passwordValidationRules.requiresDigit && !/\d/.test(password)) {
      return `${fieldName} must contain at least one digit`;
    }
    if (passwordValidationRules.requiresSpecial && !new RegExp(`[${escapedSpecials}]`).test(password)) {
      return `${fieldName} must contain at least one special character (${passwordValidationRules.specialChars})`;
    }
    return undefined;
  };

  // validate both fields (used on submit)
  const validatePasswordFields = (): PasswordErrors => {
    const newErrors: PasswordErrors = {};
    if (!currentPassword) {
      newErrors.currentPassword = 'Current password is required';
    }
    const newPasswordError = validatePassword(newPassword, 'New password');
    if (newPasswordError) {
      newErrors.newPassword = newPasswordError;
    }
    return newErrors;
  };

  // onChange validation for each field (validate only the changed field)
  const handleCurrentPasswordChange = (val: string) => {
    setCurrentPassword(val);
    setErrors((prev) => ({
      ...prev,
      currentPassword: val ? undefined : 'Current password is required'
    }));
  };

  const handleNewPasswordChange = (val: string) => {
    setNewPassword(val);
    setErrors((prev) => ({
      ...prev,
      newPassword: validatePassword(val, 'New password')
    }));
  };

  // Optional: onBlur can re-run validation for better UX (e.g., avoid showing error until blur)
  const handleCurrentPasswordBlur = () => {
    setErrors((prev) => ({
      ...prev,
      currentPassword: currentPassword ? undefined : 'Current password is required'
    }));
  };

  const handleNewPasswordBlur = () => {
    setErrors((prev) => ({
      ...prev,
      newPassword: validatePassword(newPassword, 'New password')
    }));
  };

  const handleChangePassword = async () => {
    const validationErrors = validatePasswordFields();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    try {
      setIsChangingPassword(true);
      await changePassword(currentPassword, newPassword);
      await onPasswordChanged?.();
      toast.success(successMessages.passwordChanged);
      // reset
      setErrors({});
      setCurrentPassword('');
      setNewPassword('');
    } catch (err: any) {
      console.error('Failed to change password:', err);
      if (err.response?.status === 400) {
        const message =
          err.response.data?.message ||
          'Failed to change password - Please check your current password and try again';
        toast.error(message);
      }
    } finally {
      setIsChangingPassword(false);
    }
  };

  const handleDeleteProfile = async () => {
    setShowDeleteConfirmation(true);
  };

  const handleConfirmDeleteProfile = async () => {
    try {
      setIsDeletingProfile(true);
      await deleteProfile();
      toast.success(successMessages.profileDeleted);
      localStorage.removeItem('jwtToken');
      setTimeout(() => {
        window.location.href = '/';
      }, 1500);
    } catch (err) {
      console.error('Failed to delete profile:', err);
    } finally {
      setIsDeletingProfile(false);
      setShowDeleteConfirmation(false);
    }
  };

  return (
    <>
      {/* CONFIRMATION MODAL FOR DELETE PROFILE */}
      <ConfirmationModal
        isOpen={showDeleteConfirmation}
        title="Delete Profile Permanently"
        message="Are you sure you want to permanently delete your profile? This action cannot be undone and all your data will be removed from the system."
        confirmText="Delete Profile"
        cancelText="Cancel"
        isDangerous={true}
        isLoading={isDeletingProfile}
        onConfirm={handleConfirmDeleteProfile}
        onCancel={() => setShowDeleteConfirmation(false)}
        icon={<Trash2 size={40} style={{ color: '#ef4444' }} />}
      />

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: 28 }}>
        {/* Security Settings */}
        <div>
          <div
            style={{
              background: 'white',
              borderRadius: 20,
              padding: 32,
              boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
              border: 'none'
            }}
          >
            <h3
              style={{
                fontSize: 18,
                fontWeight: 700,
                marginBottom: 24,
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                color: '#2B3674'
              }}
            >
              <AlertCircle size={20} color="#4318FF" />
              Security Settings
            </h3>

            <PasswordField
              label="Current Password"
              value={currentPassword}
              error={errors.currentPassword}
              onChange={handleCurrentPasswordChange}
              onBlur={handleCurrentPasswordBlur}
            />
            <PasswordField
              label="New Password"
              value={newPassword}
              error={errors.newPassword}
              onChange={handleNewPasswordChange}
              onBlur={handleNewPasswordBlur}
            />

            <button
              onClick={handleChangePassword}
              disabled={isChangingPassword}
              style={{
                background: 'linear-gradient(135deg, #4318FF 0%, #6C63FF 100%)',
                color: 'white',
                border: 'none',
                width: '100%',
                fontWeight: 700,
                textTransform: 'uppercase',
                marginTop: 16,
                fontSize: 12,
                padding: '12px 24px',
                borderRadius: 12,
                cursor: isChangingPassword ? 'not-allowed' : 'pointer',
                opacity: isChangingPassword ? 0.6 : 1,
                transition: 'all 0.3s ease',
                letterSpacing: '0.5px'
              }}
              onMouseEnter={(e) =>
                !isChangingPassword && (e.currentTarget.style.boxShadow = '0px 6px 16px rgba(67, 24, 255, 0.3)')
              }
              onMouseLeave={(e) => (e.currentTarget.style.boxShadow = 'none')}
            >
              {isChangingPassword ? 'Updating...' : 'Update Credentials'}
            </button>
          </div>
        </div>

        {/* Danger Zone */}
        <div>
          <div
            style={{
              background: 'rgba(238, 93, 80, 0.08)',
              borderRadius: 20,
              padding: 32,
              boxShadow: '0px 18px 40px rgba(112, 144, 176, 0.12)',
              border: '1px solid rgba(238, 93, 80, 0.2)'
            }}
          >
            <h4
              style={{
                fontSize: 16,
                fontWeight: 700,
                marginBottom: 16,
                display: 'flex',
                alignItems: 'center',
                gap: 12,
                color: '#EE5D50'
              }}
            >
              <Trash2 size={20} />
              Danger Zone
            </h4>
            <button
              onClick={handleDeleteProfile}
              disabled={isDeletingProfile}
              style={{
                background: 'rgba(238, 93, 80, 0.15)',
                color: '#EE5D50',
                border: '1px solid rgba(238, 93, 80, 0.3)',
                width: '100%',
                fontWeight: 700,
                textTransform: 'uppercase',
                fontSize: 12,
                padding: '12px 24px',
                borderRadius: 12,
                cursor: isDeletingProfile ? 'not-allowed' : 'pointer',
                opacity: isDeletingProfile ? 0.6 : 1,
                transition: 'all 0.3s ease',
                letterSpacing: '0.5px'
              }}
              onMouseEnter={(e) =>
                !isDeletingProfile && (e.currentTarget.style.background = 'rgba(238, 93, 80, 0.25)')
              }
              onMouseLeave={(e) => (e.currentTarget.style.background = 'rgba(238, 93, 80, 0.15)')}
            >
              {isDeletingProfile ? 'Deleting...' : 'Soft Delete Profile'}
            </button>
          </div>
        </div>
      </div>
    </>
  );
};