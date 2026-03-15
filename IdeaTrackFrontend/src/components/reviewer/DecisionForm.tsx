// src/components/reviewer/DecisionForm.tsx
// Bottom action bar — Accept / Reject / Send for Refinement + feedback textarea
// POST /api/reviewer/ideas/{ideaId}/decision  { feedback, decision }
// reviewerId is resolved server-side from the JWT — no client ID needed
import React, { useState } from "react";
import { submitDecision } from "../../utils/reviewerApi";
import { useShowToast } from "../../hooks/useShowToast";
import type { ReviewerDecisionRequest } from "../../utils/reviewerApi";

type DecisionOption = "ACCEPTED" | "REJECTED" | "REFINE";

interface Props {
  ideaId: number;
  onDecided?: () => void;
}

export const DecisionForm: React.FC<Props> = ({ ideaId, onDecided }) => {
  const [feedback, setFeedback] = useState("");
  const [busy, setBusy] = useState(false);
  const toast = useShowToast();

  const maxLen = 2000;

  const submit = async (decision: DecisionOption) => {
    if (!feedback.trim()) {
      toast.warning("Please enter feedback before submitting a decision.");
      return;
    }
    const payload: ReviewerDecisionRequest = {
      feedback: feedback.trim(),
      decision,
    };
    setBusy(true);
    try {
      await submitDecision(ideaId, payload);
      toast.success(`Decision "${decision}" submitted successfully!`);
      setFeedback("");
      onDecided?.();
    } catch (err: unknown) {
      toast.error(err instanceof Error ? err.message : String(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      {/* Feedback textarea */}
      <div className="mb-3">
        <label className="fb-label mb-1 d-block">
          <i className="bi bi-chat-right-text me-1 text-primary"></i>
          Feedback <span className="text-danger">*</span>
        </label>
        <textarea
          className="form-control"
          rows={3}
          maxLength={maxLen}
          placeholder="Provide feedback for the idea submitter..."
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
          style={{ fontSize: ".88rem", resize: "vertical" }}
        />
        <div
          className="text-end"
          style={{ fontSize: ".72rem", color: "var(--text-muted)" }}
        >
          {maxLen - feedback.length} chars remaining
        </div>
      </div>

      {/* Action buttons */}
      <div className="d-flex gap-2 justify-content-center flex-wrap">
        <button
          className="btn btn-success"
          disabled={busy}
          onClick={() => submit("ACCEPTED")}
        >
          {busy ? (
            <span className="spinner-border spinner-border-sm me-1"></span>
          ) : (
            <i className="bi bi-check-lg me-1"></i>
          )}
          Accept
        </button>
        <button
          className="btn btn-warning text-dark"
          disabled={busy}
          onClick={() => submit("REFINE")}
        >
          {busy ? (
            <span className="spinner-border spinner-border-sm me-1"></span>
          ) : (
            <i className="bi bi-arrow-repeat me-1"></i>
          )}
          Request Refinement
        </button>
        <button
          className="btn btn-danger"
          disabled={busy}
          onClick={() => submit("REJECTED")}
        >
          {busy ? (
            <span className="spinner-border spinner-border-sm me-1"></span>
          ) : (
            <i className="bi bi-x-lg me-1"></i>
          )}
          Reject
        </button>
      </div>
    </div>
  );
};

export default DecisionForm;
