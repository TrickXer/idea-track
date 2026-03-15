import { useEffect } from 'react';
import { X, AlertCircle, CheckCircle2, Info, AlertTriangle } from 'lucide-react';
import './Toast.css';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastMessage {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

interface ToastProps {
  toast: ToastMessage;
  onClose: (id: string) => void;
}

const getIcon = (type: ToastType) => {
  switch (type) {
    case 'success':
      return <CheckCircle2 size={20} className="text-success" />;
    case 'error':
      return <AlertCircle size={20} className="text-danger" />;
    case 'warning':
      return <AlertTriangle size={20} className="text-warning" />;
    case 'info':
    default:
      return <Info size={20} className="text-info" />;
  }
};

const getBackgroundColor = (type: ToastType) => {
  switch (type) {
    case 'success':
      return '#ECFDF5';
    case 'error':
      return '#FEF2F2';
    case 'warning':
      return '#FFFBEB';
    case 'info':
    default:
      return '#EFF3FF';
  }
};

const getBorderColor = (type: ToastType) => {
  switch (type) {
    case 'success':
      return '#10B981';
    case 'error':
      return '#EF4444';
    case 'warning':
      return '#F59E0B';
    case 'info':
    default:
      return '#4318FF';
  }
};

const getTextColor = (type: ToastType) => {
  switch (type) {
    case 'success':
      return '#065F46';
    case 'error':
      return '#7F1D1D';
    case 'warning':
      return '#78350F';
    case 'info':
    default:
      return '#1B254B';
  }
};

export const Toast = ({ toast, onClose }:ToastProps) => {
  useEffect(() => {
    if (toast.duration) {
      const timer = setTimeout(() => {
        onClose(toast.id);
      }, toast.duration);
      return () => clearTimeout(timer);
    }
  }, [toast.id, toast.duration, onClose]);

  return (
    <div
      className="toast-item"
      style={{
        background: getBackgroundColor(toast.type),
        borderLeft: `4px solid ${getBorderColor(toast.type)}`,
        color: getTextColor(toast.type),
      }}
    >
      <div className="toast-content">
        <div className="toast-icon">
          {getIcon(toast.type)}
        </div>
        <div className="toast-message">
          {toast.message}
        </div>
      </div>
      <button
        className="toast-close"
        onClick={() => onClose(toast.id)}
        style={{ color: getTextColor(toast.type) }}
      >
        <X size={18} />
      </button>
    </div>
  );
};
