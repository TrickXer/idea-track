import { useEffect } from 'react';
import { useToast } from '../context/ToastContext';
import { setupAxiosInterceptors } from '../utils/axiosInterceptor';

/**
 * Hook to setup axios error interceptor with toast notifications
 * Call this once in your App component to enable automatic error toasts
 * 
 * Example:
 * ```tsx
 * function App() {
 *   useAxiosErrorHandler();
 *   return <Router>...</Router>;
 * }
 * ```
 */
export const useAxiosErrorHandler = () => {
  const toast = useToast();

  useEffect(() => {
    // Setup interceptor
    const cleanup = setupAxiosInterceptors(toast);

    // Cleanup on unmount
    return () => {
      cleanup();
    };
  }, [toast]);
};
