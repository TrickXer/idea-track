// src/components/reviewer/DiscussionList.tsx
// GET /api/reviewer/ideas/{ideaId}/discussions/page/v2?page=&size=&sort=
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { getDiscussionsPagedV2 } from "../../utils/reviewerApi";
import { useShowToast } from "../../hooks/useShowToast";
import type {
  PagedResponse,
  ReviewerDiscussionDTO,
} from "../../utils/reviewerApi";
import { DiscussionForm } from "./DiscussionForm";

interface Props {
  ideaId: number;
  stageId?: number;
}

export const DiscussionList: React.FC<Props> = ({
  ideaId,
}) => {
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [sortAsc, setSortAsc] = useState(false); // Default to newest first (descending)
  const [data, setData] = useState<PagedResponse<ReviewerDiscussionDTO> | null>(
    null
  );
  const [loading, setLoading] = useState(false);
  const toast = useShowToast();

  const sortParam = useMemo(
    () => `createdAt,${sortAsc ? "asc" : "desc"}`,
    [sortAsc]
  );

  const load = useCallback(async () => {
    setLoading(true);
    try {
      console.log(`Loading discussions for ideaId=${ideaId}, page=${page}, size=${size}, sort=${sortParam}`);
      const res = await getDiscussionsPagedV2(
        ideaId,
        page,
        size,
        sortParam
      );
      console.log("Discussions loaded:", res);
      setData(res);
    } catch (e: unknown) {
      console.error("Failed to load discussions:", e);
      toast.error(e instanceof Error ? e.message : "Failed to load discussions");
    } finally {
      setLoading(false);
    }
  }, [ideaId, page, size, sortParam, toast]);

  useEffect(() => {
    if (ideaId) {
      load();
    }
  }, [load, ideaId]);

  const topLevel = data?.content.filter((c) => !c.replyParent) ?? [];
  const replies = (parentId: number) =>
    data?.content.filter((c) => c.replyParent === parentId) ?? [];

  return (
    <div>
      {/* Header */}
      <div className="d-flex align-items-center justify-content-between mb-3">
        <span className="fw-semibold" style={{ fontSize: ".88rem" }}>
          {data ? (
            <span
              className="text-muted"
              style={{ fontSize: ".8rem" }}
            >
              {data.totalElements} comment
              {data.totalElements !== 1 ? "s" : ""}
            </span>
          ) : null}
        </span>
        <button
          className="btn btn-sm btn-light border"
          onClick={() => {
            setSortAsc((v) => !v);
            setPage(0);
          }}
          style={{ fontSize: ".78rem" }}
        >
          <i
            className={`bi bi-sort-${sortAsc ? "up" : "down"} me-1`}
          ></i>
          {sortAsc ? "Oldest first" : "Newest first"}
        </button>
      </div>

      {loading && (
        <div className="text-center py-3 text-muted">
          <span className="spinner-border spinner-border-sm me-2"></span>
          Loading...
        </div>
      )}

      {!loading && topLevel.length === 0 && (
        <div className="it-empty py-3">
          <i className="bi bi-chat-square-dots fs-3 d-block mb-2"></i>
          No discussions yet. Start the conversation below!
        </div>
      )}

      {/* Thread list */}
      {!loading &&
        topLevel.map((item) => (
          <div key={item.userActivityId}>
            {/* Chat bubble */}
            <div className="chat-bubble">
              <div className="d-flex align-items-center gap-2 mb-2">
                <div
                  className="it-avatar"
                  style={{
                    width: 30,
                    height: 30,
                    fontSize: ".78rem",
                    flexShrink: 0,
                  }}
                >
                  {(item.displayName ?? "U").charAt(0).toUpperCase()}
                </div>
                <div>
                  <div
                    className="fw-bold"
                    style={{ fontSize: ".85rem", color: "var(--text-main)" }}
                  >
                    {item.displayName ?? "—"}
                  </div>
                  <div
                    style={{
                      fontSize: ".72rem",
                      color: "var(--text-muted)",
                    }}
                  >
                    {new Date(item.createdAt).toLocaleString("en-GB", {
                      day: "2-digit",
                      month: "short",
                      year: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                    {" · "}Stage {item.stageId}
                  </div>
                </div>
              </div>
              <p className="mb-0" style={{ fontSize: ".88rem", lineHeight: 1.6 }}>
                {item.commentText}
              </p>

              {/* Replies */}
              {replies(item.userActivityId).length > 0 && (
                <div
                  className="mt-3 ps-2"
                  style={{ borderLeft: "2px solid #e0e5f2" }}
                >
                  {replies(item.userActivityId).map((r) => (
                    <div key={r.userActivityId} className="chat-bubble is-reply">
                      <div className="d-flex align-items-center gap-2 mb-1">
                        <div
                          className="it-avatar"
                          style={{
                            width: 24,
                            height: 24,
                            fontSize: ".7rem",
                            flexShrink: 0,
                          }}
                        >
                          {(r.displayName ?? "U").charAt(0).toUpperCase()}
                        </div>
                        <div>
                          <span
                            className="fw-bold"
                            style={{ fontSize: ".82rem" }}
                          >
                            {r.displayName}
                          </span>
                          <span
                            className="ms-2"
                            style={{
                              fontSize: ".7rem",
                              color: "var(--text-muted)",
                            }}
                          >
                            {new Date(r.createdAt).toLocaleString("en-GB", {
                              day: "2-digit",
                              month: "short",
                              hour: "2-digit",
                              minute: "2-digit",
                            })}
                          </span>
                        </div>
                      </div>
                      <p className="mb-0" style={{ fontSize: ".85rem" }}>
                        {r.commentText}
                      </p>
                    </div>
                  ))}
                </div>
              )}

              {/* Inline reply form */}
              <details className="mt-2">
                <summary
                  className="text-primary fw-semibold"
                  style={{
                    fontSize: ".78rem",
                    cursor: "pointer",
                    userSelect: "none",
                  }}
                >
                  <i className="bi bi-reply me-1"></i>
                  Reply{" "}
                  {replies(item.userActivityId).length > 0 &&
                    `(${replies(item.userActivityId).length})`}
                </summary>
                <div className="mt-2">
                  <DiscussionForm
                    ideaId={ideaId}
                    replyParent={item.userActivityId}
                    onPosted={load}
                  />
                </div>
              </details>
            </div>
          </div>
        ))}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="d-flex align-items-center gap-2 mt-3">
          <button
            className="btn btn-sm btn-outline-secondary"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={data.first}
          >
            <i className="bi bi-chevron-left"></i> Prev
          </button>
          <span className="text-muted" style={{ fontSize: ".82rem" }}>
            Page {data.page + 1} / {data.totalPages}
          </span>
          <button
            className="btn btn-sm btn-outline-secondary"
            onClick={() =>
              setPage((p) => Math.min(data.totalPages - 1, p + 1))
            }
            disabled={data.last}
          >
            Next <i className="bi bi-chevron-right"></i>
          </button>
        </div>
      )}

      {/* New comment composer */}
      <div className="mt-4 pt-3 border-top">
        <p
          className="fw-semibold mb-2"
          style={{ fontSize: ".85rem", color: "var(--text-main)" }}
        >
          <i className="bi bi-pencil-square me-1 text-primary"></i>Add a comment
        </p>
        <DiscussionForm
          ideaId={ideaId}
          onPosted={load}
        />
      </div>
    </div>
  );
};

export default DiscussionList;
