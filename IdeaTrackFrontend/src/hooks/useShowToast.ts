import { useMemo } from 'react';
import { useToast } from '../context/ToastContext';

/**
 * Hook to show toast notifications
 * Usage: const toast = useShowToast();
 *        toast.success('Operation successful');
 *        toast.error('An error occurred');
 */
export const useShowToast = () => {
  const { addToast } = useToast();

  return useMemo(() => ({
    success: (message: string, duration: number = 5000) =>
      addToast(message, 'success', duration),
    error: (message: string, duration: number = 5000) =>
      addToast(message, 'error', duration),
    warning: (message: string, duration: number = 5000) =>
      addToast(message, 'warning', duration),
    info: (message: string, duration: number = 5000) =>
      addToast(message, 'info', duration),
  }), [addToast]);
};
