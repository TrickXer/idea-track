// src/components/Timeline.tsx
// Vertical stepper — shows idea progression through review stages.
// Common component used across multiple pages (IdeaHierarchy, ReviewerIdeaPage, etc.)
// Receives ProgressionDTO { ideaId, currentStatus, steps[], bars[] }
// REFINE is NOT a timeline step — it is shown as a side callout card.
// Rejection path: green connector bar all the way to REJECTED (sv-bar-filled
//   on intermediate nodes that were passed-through), REJECTED node = solid red.
import React from "react";
import type {
  ProgressionDTO,
  ProgressionStep,
} from "../../utils/reviewerApi";

interface Props {
  data: ProgressionDTO | null;
}

function fmtDate(at?: string | null): string {
  if (!at) return "";
  try {
    return new Date(at).toLocaleString("en-GB", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return at;
  }
}

/**
 * A step is "bypassed" when the idea has progressed PAST it
 * (a later step is active or reached) but this step was never reached itself.
 * Bypassed steps display "Not Required".
 */
function isBypassed(step: ProgressionStep, steps: ProgressionStep[]): boolean {
  if (step.reached || step.active) return false;
  const stepIdx = steps.indexOf(step);
  return steps.slice(stepIdx + 1).some((s) => s.reached || s.active);
}

export const Timeline: React.FC<Props> = ({ data }) => {
  if (!data) {
    return (
      <div className="it-empty py-4">
        <span className="spinner-border spinner-border-sm me-2 text-primary"></span>
        Loading progression...
      </div>
    );
  }

  // Separate out REFINE from the main step list
  const refineStep = data.steps.find((s) => s.key === "REFINE");
  const mainSteps = data.steps.filter((s) => s.key !== "REFINE");

  const isRejected = data.currentStatus === "REJECTED";
  const isPositive =
    data.currentStatus === "ACCEPTED" || data.currentStatus === "APPROVED";

  // Status badge colours
  const statusBg =
    isPositive
      ? "#d1f0e0"
      : isRejected
      ? "#fde8e8"
      : data.currentStatus === "REFINE"
      ? "#fff0d9"
      : "#e8eeff";
  const statusColor =
    isPositive
      ? "#0a6640"
      : isRejected
      ? "#9c1616"
      : data.currentStatus === "REFINE"
      ? "#8a4500"
      : "#3050cc";

  return (
    <div>
      <div className="d-flex align-items-center justify-content-between mb-3">
        <span
          className="fw-bold"
          style={{ fontSize: ".9rem", color: "var(--text-main)" }}
        >
          <i className="bi bi-diagram-3 me-2 text-primary"></i>Progression
        </span>
        <span className="sp" style={{ background: statusBg, color: statusColor }}>
          {data.currentStatus}
        </span>
      </div>

      {/* REFINE callout — shown only when the current status IS 'REFINE' */}
      {refineStep && data.currentStatus === "REFINE" && (
        <div className="refine-marker mb-3">
          <div className="refine-marker-header">
            <i className="bi bi-arrow-repeat me-1"></i>
            Refinement Requested
            {refineStep.at && (
              <span className="refine-marker-date">
                <i className="bi bi-clock me-1"></i>
                {fmtDate(refineStep.at)}
              </span>
            )}
          </div>
          <div className="refine-marker-body">
            {refineStep.reached
              ? "This idea was sent back to the submitter for refinement."
              : "Pending refinement — awaiting submitter response."}
          </div>
        </div>
      )}

      <ul className="stepper-v">
        {mainSteps.map((step, idx) => {
          const bypassed = isBypassed(step, mainSteps);
          const isRejNode = step.key === "REJECTED";

          // ── Bar (connector line) colouring ──────────────────────────────
          // On the rejection path: every step that is reached OR bypassed
          // gets a green bar. The REJECTED node itself is the terminal.
          const barFilled = step.reached || step.active || bypassed;

          // ── Node (::before circle) class ─────────────────────────────────
          // sv-done      = green filled  (completed / positive terminal)
          // sv-curr      = red pulsing   (active in-progress, non-positive, non-rejected path)
          // sv-rejected  = solid red dot, green bar (terminal REJECTED node)
          // sv-bar-filled = green bar only, gray outline node (bypassed / pass-through)
          // (default)    = gray outline ring (pending)
          let nodeClass = "";

          if (isRejNode && step.active) {
            // Terminal REJECTED: red dot, green bar (sv-bar-filled added below)
            nodeClass = "sv-rejected";
          } else if (step.reached && !step.active) {
            // Fully completed step — green dot + green bar
            nodeClass = "sv-done";
          } else if (step.active) {
            // Active step: green when idea has reached a terminal (rejected or positive)
            // Use sv-curr (red) ONLY when still genuinely in-progress (UNDERREVIEW)
            const atTerminal = isPositive || isRejected;
            nodeClass = atTerminal ? "sv-done" : "sv-curr";
          }
          // bypassed steps get sv-bar-filled (green line, gray node) via liClassName below

          const liClassName = [
            "step-v-item",
            nodeClass,
            // Green bar: bypassed/pass-through steps OR the REJECTED terminal node
            (barFilled && !step.reached && !step.active) ||
            (isRejNode && step.active)
              ? "sv-bar-filled"
              : "",
          ]
            .filter(Boolean)
            .join(" ");

          // Icon inside label row
          let labelIcon: React.ReactNode;
          if (isRejNode && step.active) {
            // Solid red X for rejected terminal
            labelIcon = (
              <i
                className="bi bi-x-circle-fill me-1"
                style={{ color: "var(--step-cur)", fontSize: ".75rem" }}
              ></i>
            );
          } else if (step.reached || step.active) {
            labelIcon = (
              <i
                className="bi bi-check-circle-fill me-1"
                style={{ color: "var(--step-done)", fontSize: ".75rem" }}
              ></i>
            );
          } else {
            // Pending / bypassed: outline circle
            labelIcon = (
              <i
                className="bi bi-circle me-1"
                style={{ color: "var(--step-idle)", fontSize: ".75rem" }}
              ></i>
            );
          }

          return (
            <li key={step.key ?? idx} className={liClassName}>
              <div className="step-v-label">
                {labelIcon}
                {step.label}
                {step.active && (
                  <span
                    className="ms-2 badge"
                    style={{
                      background: isRejNode
                        ? "rgba(239,68,68,.15)"
                        : "rgba(16,185,129,.15)",
                      color: isRejNode
                        ? "var(--step-cur)"
                        : "var(--step-done)",
                      fontSize: ".65rem",
                      borderRadius: 20,
                      padding: "2px 8px",
                      fontWeight: 700,
                    }}
                  >
                    {isRejNode ? "Rejected" : "Current"}
                  </span>
                )}
              </div>

              {step.at ? (
                <div className="step-v-time">
                  <i className="bi bi-clock me-1"></i>
                  {fmtDate(step.at)}
                </div>
              ) : bypassed ? (
                <div
                  className="step-v-time"
                  style={{ color: "#a3aed0", fontStyle: "italic" }}
                >
                  Not Required
                </div>
              ) : (
                <div className="step-v-time">Pending</div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
};

export default Timeline;
