// src/components/ConfirmationModal.tsx
import React from "react";
import { AlertCircle } from "lucide-react";
import "./ConfirmationModal.css";

export interface ConfirmationModalProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  isDangerous?: boolean;
  isLoading?: boolean;
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
  icon?: React.ReactNode;
}

const ConfirmationModal: React.FC<ConfirmationModalProps> = ({
  isOpen,
  title,
  message,
  confirmText = "Confirm",
  cancelText = "Cancel",
  isDangerous = false,
  isLoading = false,
  onConfirm,
  onCancel,
  icon,
}) => {
  if (!isOpen) return null;

  const handleConfirm = async () => {
    try {
      await onConfirm();
    } catch (error) {
      console.error("Confirmation action failed:", error);
    }
  };

  return (
    <div className="confirmation-modal-overlay" onClick={onCancel}>
      <div
        className="confirmation-modal-card"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="confirmation-modal-icon-section">
          {icon || <AlertCircle size={40} className="confirmation-modal-icon" />}
        </div>

        <h2 className="confirmation-modal-title">{title}</h2>
        <p className="confirmation-modal-message">{message}</p>

        <div className="confirmation-modal-actions">
          <button
            className="btn-cancel"
            onClick={onCancel}
            disabled={isLoading}
          >
            {cancelText}
          </button>
          <button
            className={`btn-confirm ${isDangerous ? "btn-danger" : "btn-primary"}`}
            onClick={handleConfirm}
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <span
                  className="spinner-border spinner-border-sm"
                  role="status"
                  aria-hidden="true"
                  style={{ marginRight: "8px" }}
                ></span>
                Loading...
              </>
            ) : (
              confirmText
            )}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmationModal;
