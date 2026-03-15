// src/components/reviewer/DiscussionForm.tsx
// POST /api/reviewer/ideas/{ideaId}/discussions/v2
// Body: { text, replyParent? }
// userId is extracted from JWT by backend, stageId is derived from AssignedReviewerToIdea
import React, { useState } from "react";
import { postDiscussionV2 } from "../../utils/reviewerApi";
import { useShowToast } from "../../hooks/useShowToast";

interface Props {
  ideaId: number;
  stageId?: number;
  userId?: number;
  replyParent?: number | null;
  onPosted?: () => void;
}

export const DiscussionForm: React.FC<Props> = ({
  ideaId,
  replyParent = null,
  onPosted,
}) => {
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const toast = useShowToast();

  const maxLen = 2000;
  const remaining = maxLen - text.length;

  const handlePost = async () => {
    if (!text.trim()) {
      toast.warning("Comment cannot be empty.");
      return;
    }

    setBusy(true);
    try {
      await postDiscussionV2(ideaId, text.trim(), replyParent);
      setText("");
      toast.success("Comment posted successfully!");
      onPosted?.();
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className={`mt-2${replyParent ? " ms-4 ps-3 border-start" : ""}`}>
      <textarea
        className="form-control form-control-sm"
        rows={replyParent ? 2 : 3}
        maxLength={maxLen}
        placeholder={replyParent ? "Write a reply…" : "Write a comment…"}
        value={text}
        onChange={(e) => setText(e.target.value)}
        style={{ fontSize: ".85rem", resize: "vertical" }}
      />
      <div className="d-flex align-items-center justify-content-between mt-1 gap-2">
        <span className="text-muted" style={{ fontSize: ".75rem" }}>
          {remaining} chars left
        </span>
        <button
          className="btn btn-sm btn-primary"
          onClick={handlePost}
          disabled={busy || !text.trim()}
        >
          {busy ? (
            <>
              <span className="spinner-border spinner-border-sm me-1"></span>
              Posting…
            </>
          ) : replyParent ? (
            <>
              <i className="bi bi-reply me-1"></i>Reply
            </>
          ) : (
            <>
              <i className="bi bi-send me-1"></i>Post
            </>
          )}
        </button>
      </div>
    </div>
  );
};

export default DiscussionForm;
