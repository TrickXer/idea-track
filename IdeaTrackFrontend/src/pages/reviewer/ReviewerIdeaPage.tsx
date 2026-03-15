// src/pages/reviewer/ReviewerIdeaPage.tsx
// IdeaDetailPanel — inline detail view (used by ReviewerDashboard + route)
// Also exports ReviewerIdeaPage as a route-based wrapper for /reviewer/ideas/:ideaId
import "./reviewer.css";
import React, { useCallback, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Timeline } from "../../components/idea/Timeline";
import { DecisionForm } from "../../components/reviewer/DecisionForm";
import { DiscussionList } from "../../components/reviewer/DiscussionList";
import { getDecisions, getIdeaProgression } from "../../utils/reviewerApi";
import { getIdeaDetails } from "../../utils/ideaApi";
import { fetchMyProfile } from "../../utils/profileApi";
import { useShowToast } from "../../hooks/useShowToast";
import type {
  ProgressionDTO,
  ReviewerDecisionRequest,
} from "../../utils/reviewerApi";

// ─── IdeaDetailPanel ──────────────────────────────────────────────────────────

interface PanelProps {
  ideaId: number;
  ideaTitle?: string;
  onBack: () => void;
}

export const IdeaDetailPanel: React.FC<PanelProps> = ({
  ideaId,
  ideaTitle,
  onBack,
}) => {
  const [progression, setProgression] = useState<ProgressionDTO | null>(null);
  const [ideaDetails, setIdeaDetails] = useState<any>(null);
  const [history, setHistory] = useState<ReviewerDecisionRequest[]>([]);
  const [userId, setUserId] = useState<number>(0);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<
    "overview" | "discussion" | "history"
  >("overview");
  const toast = useShowToast();

  const loadAll = useCallback(async () => {
    if (!ideaId || isNaN(ideaId)) return;
    setBusy(true);
    setErr(null);
    try {
      const [p, h, profile, ideaDetailsRes] = await Promise.all([
        getIdeaProgression(ideaId),
        getDecisions(ideaId),
        fetchMyProfile(),
        getIdeaDetails(ideaId),
      ]);
      setProgression(p);
      setIdeaDetails(ideaDetailsRes.data);
      setHistory(h as ReviewerDecisionRequest[]);
      setUserId(profile.userId);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Failed to load idea details";
      setErr(msg);
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  }, [ideaId]);

  useEffect(() => {
    loadAll();
  }, [loadAll]);

  // Find active stage ID for discussions
  const activeStageId: number | null = (() => {
    if (!progression?.steps) return null;
    const activeStep = progression.steps.find((s) => s.active);
    if (activeStep) {
      const idx = progression.steps.indexOf(activeStep);
      return idx + 1;
    }
    const stageId = progression.steps.length > 0 ? progression.steps.length : null;
    console.log("Calculated activeStageId:", stageId, "from progression:", progression);
    return stageId;
  })();

  return (
    <div className="detail-view-wrap">
      {/* ── Header ── */}
      <div className="d-flex align-items-center gap-3 mb-4 flex-wrap">
        <button
          className="btn btn-sm btn-light border"
          onClick={onBack}
          style={{ fontWeight: 600 }}
        >
          <i className="bi bi-arrow-left me-1"></i>Back
        </button>
        <div className="flex-fill">
          <h4
            className="fw-bold mb-0"
            style={{ color: "var(--text-main)", fontSize: "1.1rem" }}
          >
            <i className="bi bi-lightbulb me-2 text-warning"></i>
            {ideaTitle ?? `Idea #${ideaId}`}
          </h4>
          <small style={{ color: "var(--text-muted)" }}>
            Idea ID #{ideaId}
          </small>
        </div>
        <button
          className="btn btn-sm btn-outline-primary"
          onClick={loadAll}
          disabled={busy}
        >
          <i
            className={`bi bi-arrow-clockwise me-1${busy ? " spin" : ""}`}
          ></i>
          {busy ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      {/* Error */}
      {err && (
        <div
          className="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3"
          style={{ fontSize: ".87rem" }}
        >
          <i className="bi bi-exclamation-triangle-fill"></i>
          <span className="flex-fill">{err}</span>
          <button className="btn btn-sm btn-danger" onClick={loadAll}>
            Retry
          </button>
        </div>
      )}

      {busy && !progression && (
        <div className="text-center py-5 text-muted">
          <span className="spinner-border me-2 text-primary"></span>
          Loading...
        </div>
      )}

      {/* ── Tab buttons ── */}
      <div className="d-flex gap-2 mb-4 flex-wrap">
        {(["overview", "discussion", "history"] as const).map((tab) => (
          <button
            key={tab}
            className={`btn btn-sm ${
              activeTab === tab ? "btn-primary" : "btn-outline-secondary"
            }`}
            style={{
              borderRadius: 20,
              fontWeight: 600,
              fontSize: ".82rem",
              padding: ".3rem 1rem",
            }}
            onClick={() => setActiveTab(tab)}
          >
            {tab === "overview" && (
              <>
                <i className="bi bi-file-text me-1"></i>Overview
              </>
            )}
            {tab === "discussion" && (
              <>
                <i className="bi bi-chat-left-text me-1"></i>Discussions
              </>
            )}
            {tab === "history" && (
              <>
                <i className="bi bi-clock-history me-1"></i>Decision History
              </>
            )}
          </button>
        ))}
      </div>

      {/* ── Main content grid ── */}
      <div className="row g-4">
        {/* Left column — tab content */}
        <div className="col-12 col-md-8">
          {/* OVERVIEW TAB */}
          {activeTab === "overview" && (
            <div>
              {/* Thumbnail Link Button */}
              {ideaDetails?.thumbnailURL && (
                <div className="mb-4">
                  <a
                    href={ideaDetails.thumbnailURL}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="btn btn-outline-primary btn-sm"
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "8px",
                      fontSize: ".85rem",
                      padding: ".5rem 1rem",
                    }}
                  >
                    <i className="bi bi-image"></i>
                    View Thumbnail
                  </a>
                </div>
              )}

              {/* Idea Title & Description */}
              <div
                className="mb-4 p-3"
                style={{
                  background: "#f8faff",
                  borderRadius: 12,
                  border: "1px solid #edf0f9",
                }}
              >
                <p
                  className="fw-semibold mb-2"
                  style={{ fontSize: ".9rem", color: "var(--text-main)" }}
                >
                  <i className="bi bi-lightbulb me-1 text-warning"></i>
                  {ideaDetails?.title || progression?.ideaTitle || `Idea #${ideaId}`}
                </p>
                {ideaDetails?.description && (
                  <p
                    className="text-muted mb-0"
                    style={{ fontSize: ".88rem", lineHeight: 1.5 }}
                  >
                    {ideaDetails.description}
                  </p>
                )}
              </div>

              {/* About This Idea */}
              <div
                className="mb-4 p-3"
                style={{
                  background: "#f8faff",
                  borderRadius: 12,
                  border: "1px solid #edf0f9",
                }}
              >
                <p
                  className="fw-semibold mb-1"
                  style={{ fontSize: ".85rem", color: "var(--text-main)" }}
                >
                  <i className="bi bi-card-text me-1 text-primary"></i>About
                  this idea
                </p>
                <p
                  className="text-muted mb-0"
                  style={{ fontSize: ".88rem" }}
                >
                  Review and provide feedback on this idea
                </p>
              </div>
            </div>
          )}

          {/* DISCUSSIONS TAB */}
          {activeTab === "discussion" && (
            <div>
              <DiscussionList
                ideaId={ideaId}
              />
            </div>
          )}

          {/* DECISION HISTORY TAB */}
          {activeTab === "history" && (
            <div>
              <p
                className="fw-semibold mb-3"
                style={{ fontSize: ".88rem", color: "var(--text-main)" }}
              >
                <i className="bi bi-clock-history me-1 text-primary"></i>
                Decision History
              </p>
              {history.length === 0 ? (
                <div className="it-empty py-4">
                  <i className="bi bi-inbox fs-3 d-block mb-2"></i>
                  No decisions recorded yet.
                </div>
              ) : (
                history.map((d, i) => (
                  <div key={i} className="chat-bubble mb-2">
                    <div className="d-flex align-items-center gap-2 mb-1">
                      <span
                        className="fw-bold"
                        style={{ fontSize: ".83rem" }}
                      >
                        <i className="bi bi-person-circle me-1"></i>
                        Reviewer #{d.reviewerId}
                      </span>
                      {d.decision && (
                        <span
                          className="sp"
                          style={{
                            background:
                              d.decision === "ACCEPTED"
                                ? "#d1f0e0"
                                : d.decision === "REJECTED"
                                ? "#fde8e8"
                                : "#fff0d9",
                            color:
                              d.decision === "ACCEPTED"
                                ? "#0a6640"
                                : d.decision === "REJECTED"
                                ? "#9c1616"
                                : "#8a4500",
                          }}
                        >
                          {d.decision}
                        </span>
                      )}
                    </div>
                    <p
                      className="mb-0"
                      style={{ fontSize: ".88rem", lineHeight: 1.6 }}
                    >
                      {d.feedback || (
                        <span className="text-muted fst-italic">
                          No feedback.
                        </span>
                      )}
                    </p>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Right column — vertical stepper */}
        <div
          className="col-12 col-md-4 detail-right-col"
          style={{ borderLeft: "1px solid #edf0f9", paddingLeft: "1.5rem" }}
        >
          <Timeline data={progression} />
        </div>
      </div>

      {/* ── Bottom action bar ── */}
      <div className="mt-4 pt-4" style={{ borderTop: "1px solid #edf0f9" }}>
        <p
          className="fw-bold mb-3"
          style={{ fontSize: ".9rem", color: "var(--text-main)" }}
        >
          <i className="bi bi-clipboard2-check me-2 text-primary"></i>Submit
          Decision
        </p>
        <DecisionForm ideaId={ideaId} onDecided={loadAll} />
      </div>
    </div>
  );
};

// ─── ReviewerIdeaPage (route wrapper) ─────────────────────────────────────────

export const ReviewerIdeaPage: React.FC = () => {
  const { ideaId } = useParams<{ ideaId: string }>();
  const id = Number(ideaId);

  return (
    <div className="container-fluid py-4 px-3 px-md-4">
      <IdeaDetailPanel ideaId={id} onBack={() => window.history.back()} />
    </div>
  );
};

export default ReviewerIdeaPage;
