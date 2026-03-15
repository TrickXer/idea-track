import axios from 'axios';
import { useToast } from '../context/ToastContext';

interface ApiErrorResponse {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  path?: string;
  validationErrors?: Record<string, string>;
}

/**
 * API error interceptor that shows toast notifications for errors
 * Handles all backend exception types from GlobalExceptionHandler
 * 
 * Maps backend errors to user-friendly toast messages:
 * - 400: Validation/Bad Request errors
 * - 401: Authentication errors (Invalid/Expired token)
 * - 403: Authorization/Forbidden errors
 * - 404: Not Found errors
 * - 409: Conflict/Duplicate errors
 * - 422: Validation errors with field details
 * - 500: Server/Internal errors
 */
export const setupAxiosInterceptors = (toast: ReturnType<typeof useToast>) => {
  const errorInterceptor = axios.interceptors.response.use(
    response => response,
    error => {
      let errorMessage = 'An unexpected error occurred';
      let errorType: 'error' | 'warning' = 'error';

      if (error.response) {
        const { status, data } = error.response;
        const apiError = data as ApiErrorResponse;

        // ============================
        // 401 - Unauthorized / Authentication
        // ============================
        if (status === 401) {
          if (apiError.message?.includes('Token expired') || apiError.message?.includes('expired')) {
            errorMessage = '🔑 Session expired - Please log in again';
          } else if (apiError.message?.includes('Invalid token')) {
            errorMessage = '🔐 Invalid authentication token - Please log in again';
          } else if (apiError.message?.includes('Unauthorized')) {
            errorMessage = '⛔ Unauthorized - Access denied';
          } else if (apiError.message?.includes('Bad credentials') || apiError.message?.includes('incorrect')) {
            errorMessage = '❌ Invalid email or password';
          } else {
            errorMessage = apiError.message || 'Authentication failed - Please log in again';
          }
        }

        // ============================
        // 403 - Forbidden / Authorization
        // ============================
        else if (status === 403) {
          errorMessage = apiError.message || '🚫 You do not have permission to perform this action';
          errorType = 'warning';
        }

        // ============================
        // 404 - Not Found
        // ============================
        else if (status === 404) {
          if (apiError.message?.includes('User')) {
            errorMessage = '👤 User not found';
          } else if (apiError.message?.includes('Idea')) {
            errorMessage = '💡 Idea not found';
          } else if (apiError.message?.includes('Category')) {
            errorMessage = '📂 Category not found';
          } else if (apiError.message?.includes('Department')) {
            errorMessage = '🏢 Department not found';
          } else if (apiError.message?.includes('Profile')) {
            errorMessage = '📋 Profile not found';
          } else if (apiError.message?.includes('Resource')) {
            errorMessage = '📦 Resource not found';
          } else {
            errorMessage = apiError.message || 'Resource not found';
          }
          errorType = 'warning';
        }

        // ============================
        // 409 - Conflict / Duplicate
        // ============================
        else if (status === 409) {
          if (apiError.message?.includes('Duplicate') || apiError.message?.includes('already exists')) {
            errorMessage = '⚠️ ' + (apiError.message || 'This resource already exists');
          } else if (apiError.message?.includes('email')) {
            errorMessage = '📧 Email already registered';
          } else if (apiError.message?.includes('Category')) {
            errorMessage = '📂 Category already exists';
          } else {
            errorMessage = apiError.message || 'Conflict - Resource already exists';
          }
          errorType = 'warning';
        }

        // ============================
        // 422 - Validation Errors
        // ============================
        else if (status === 422) {
          if (apiError.validationErrors && Object.keys(apiError.validationErrors).length > 0) {
            const errors = Object.entries(apiError.validationErrors)
              .map(([field, msg]) => `${field}: ${msg}`)
              .join('; ');
            errorMessage = `✓ Validation error: ${errors}`;
          } else {
            errorMessage = apiError.message || 'Validation failed';
          }
          errorType = 'warning';
        }

        // ============================
        // 400 - Bad Request / Validation
        // ============================
        else if (status === 400) {
          // Check for field validation errors first
          if (apiError.validationErrors && Object.keys(apiError.validationErrors).length > 0) {
            const firstError = Object.values(apiError.validationErrors)[0];
            errorMessage = `✓ ${firstError}`;
          }
          // Password-related errors
          else if (apiError.message?.includes('Password too weak')) {
            errorMessage = '🔐 Password too weak! Must contain uppercase, lowercase, digit, and special character (@#$%^&+=!)';
            errorType = 'warning';
          }
          else if (apiError.message?.includes('password') || apiError.message?.includes('Password')) {
            errorMessage = apiError.message || '🔐 Password error - Please check your current password and try again';
            errorType = 'warning';
          }
          // File upload errors
          else if (apiError.message?.includes('File too large')) {
            errorMessage = '📁 File too large - Maximum 5 MB allowed';
            errorType = 'warning';
          }
          else if (apiError.message?.includes('Invalid file type')) {
            errorMessage = '📁 Invalid file type - Only JPEG and PNG are allowed';
            errorType = 'warning';
          }
          else if (apiError.message?.includes('Invalid file')) {
            errorMessage = apiError.message;
            errorType = 'warning';
          }
          // Profile/General errors
          else if (apiError.message?.includes('Name cannot be empty')) {
            errorMessage = '✓ Name cannot be empty';
            errorType = 'warning';
          }
          else if (apiError.message?.includes('Phone number')) {
            errorMessage = '✓ Invalid phone number format';
            errorType = 'warning';
          }
          else if (apiError.message?.includes('Malformed request')) {
            errorMessage = '✓ Invalid request format';
          }
          else if (apiError.message) {
            errorMessage = apiError.message;
            errorType = 'warning';
          }
          else {
            errorMessage = '⚠️ Invalid request - Please check your input';
            errorType = 'warning';
          }
        }

        // ============================
        // 500 - Internal Server Error
        // ============================
        else if (status === 500) {
          if (apiError.message?.includes('Hierarchy')) {
            errorMessage = '🏗️ Failed to build idea hierarchy - Please try again';
          } else if (apiError.message?.includes('Profile')) {
            errorMessage = '👤 Failed to process profile - Please try again';
          } else if (apiError.message?.includes('Gamification')) {
            errorMessage = '🎮 Failed to update gamification data - Please try again';
          } else {
            errorMessage = '⚠️ Server error - Please try again later';
          }
        }

        // Fallback for any other status codes with response
        else {
          errorMessage = apiError.message || `Error (${status}): ${apiError.error || 'Unknown error'}`;
        }
      } 
      // ============================
      // Network / Request errors
      // ============================
      else if (error.request) {
        errorMessage = '🌐 No response from server - Check your connection';
      } 
      // ============================
      // Request setup errors
      // ============================
      else {
        errorMessage = error.message || 'An error occurred';
      }

      // Show error toast with appropriate type
      const duration = 5000;  // All toasts appear for 5 seconds
      toast.addToast(errorMessage, errorType, duration);

      return Promise.reject(error);
    }
  );

  // Return cleanup function to remove interceptor
  return () => {
    axios.interceptors.response.eject(errorInterceptor);
  };
};

/**
 * Success messages for common operations
 * Use these after successful API calls to provide feedback
 */
export const successMessages = {
  // Profile operations
  profileUpdated: '✅ Profile updated successfully',
  photoUploaded: '✅ Profile photo updated successfully',
  photoDeleted: '✅ Profile photo deleted',
  passwordChanged: '✅ Password changed successfully',
  profileDeleted: '✅ Profile deleted successfully',

  // Idea operations
  ideaCreated: '✅ Idea created successfully',
  ideaUpdated: '✅ Idea updated successfully',
  ideaSubmitted: '✅ Idea submitted for review',
  ideaDeleted: '✅ Idea deleted',

  // Review operations
  reviewSubmitted: '✅ Review submitted',
  reviewUpdated: '✅ Review updated',
  decisionRecorded: '✅ Decision recorded',

  // Proposal operations
  proposalCreated: '✅ Proposal created successfully',
  proposalUpdated: '✅ Proposal updated successfully',
  proposalSubmitted: '✅ Proposal submitted',

  // General operations
  operationSuccess: '✅ Operation completed successfully',
  dataSaved: '✅ Data saved successfully',
  dataCopied: '✅ Copied to clipboard',
  uploadSuccess: '✅ Upload successful',
};
