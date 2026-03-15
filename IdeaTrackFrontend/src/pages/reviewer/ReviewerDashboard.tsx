// src/pages/reviewer/ReviewerDashboard.tsx
// Screen.html-aligned design: inline detail panel (no navigation), deduplication by ideaId
import "./reviewer.css";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { getReviewerDashboard, runEodAssignmentNow } from "../../utils/reviewerApi";
import { useShowToast } from "../../hooks/useShowToast";
import type { ReviewerDashboardDTO } from "../../utils/reviewerApi";
import { useAuth } from "../../utils/authContext";
import { IdeaDetailPanel } from "./ReviewerIdeaPage";

const FILTERS = [
  "ALL",
  "UNDERREVIEW",
  "ACCEPTED",
  "REJECTED",
  "REFINE",
  "PENDING",
] as const;
type FilterType = (typeof FILTERS)[number];

function statusPill(val?: string | null): string {
  const v = (val ?? "PENDING").toUpperCase();
  return `sp sp-${v}`;
}

function displayStatus(r: ReviewerDashboardDTO): string {
  const ideaStatus = (r.currentIdeaStatus ?? "").toUpperCase();
  // Always show the actual idea status when it's a meaningful terminal/active state
  if (ideaStatus && ideaStatus !== "SUBMITTED") return ideaStatus;
  // Fall back to reviewer's own decision only when idea hasn't progressed yet
  const d = (r.reviewerDecision ?? "").toUpperCase();
  if (d) return d;
  return "UNDERREVIEW";
}

const ReviewerDashboard: React.FC = () => {
  const { roles } = useAuth();
  const toast = useShowToast();

  const [filter, setFilter] = useState<FilterType>("ALL");
  const [rows, setRows] = useState<ReviewerDashboardDTO[]>([]);
  const [search, setSearch] = useState("");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);

  const isAdmin =
    roles.includes("ADMIN") || roles.includes("SUPERADMIN");

  const load = useCallback(async () => {
    setBusy(true);
    try {
      const data = await getReviewerDashboard(filter);
      setRows(data);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Failed to load dashboard";
      setErr(msg);
      toast.error(msg);
    } finally {
      setBusy(false);
    }
  }, [filter, toast]);

  useEffect(() => {
    load();
  }, [load]);

  // -- Deduplication: one row per ideaId, keep highest assignmentStage --
  const deduplicated = useMemo(() => {
    const map = new Map<number, ReviewerDashboardDTO>();
    rows.forEach((r) => {
      const existing = map.get(r.ideaId);
      if (
        !existing ||
        (r.assignmentStage ?? 0) > (existing.assignmentStage ?? 0)
      ) {
        map.set(r.ideaId, r);
      }
    });
    return Array.from(map.values());
  }, [rows]);

  // -- Client-side search --
  const filtered = useMemo(() => {
    if (!search.trim()) return deduplicated;
    const q = search.toLowerCase();
    return deduplicated.filter(
      (r) =>
        String(r.ideaId).includes(q) ||
        (r.ideaTitle ?? "").toLowerCase().includes(q) ||
        (r.employeeName ?? "").toLowerCase().includes(q) ||
        (r.categoryName ?? "").toLowerCase().includes(q)
    );
  }, [deduplicated, search]);

  // -- Stats (over deduplicated) --
  const stats = useMemo(() => {
    const total = deduplicated.length;
    const accepted = deduplicated.filter(
      (r) => displayStatus(r) === "ACCEPTED"
    ).length;
    const rejected = deduplicated.filter(
      (r) => displayStatus(r) === "REJECTED"
    ).length;
    const refine = deduplicated.filter(
      (r) => displayStatus(r) === "REFINE"
    ).length;
    const pending = deduplicated.filter((r) => {
      const s = displayStatus(r);
      return s === "UNDERREVIEW" || s === "PENDING" || s === "SUBMITTED";
    }).length;
    return { total, accepted, rejected, refine, pending };
  }, [deduplicated]);

  const handleEod = async () => {
    try {
      await runEodAssignmentNow();
      toast.success("EOD assignment job triggered successfully.");
    } catch (e: unknown) {
      toast.error(e instanceof Error ? e.message : "EOD job failed");
    }
  };

  // -- Detail panel view --
  if (selectedId !== null) {
    return (
      <div className="container-fluid py-4 px-3 px-md-4">
        <IdeaDetailPanel
          ideaId={selectedId}
          ideaTitle={rows.find((r) => r.ideaId === selectedId)?.ideaTitle}
          onBack={() => setSelectedId(null)}
        />
      </div>
    );
  }

  // -- Dashboard table view --
  return (
    <div className="container-fluid py-4 px-3 px-md-4">
      {/* Page title */}
      <div className="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <div>
          <h2
            className="fw-bold mb-1"
            style={{ fontSize: "1.35rem", color: "var(--text-main)" }}
          >
            Reviewer Dashboard
          </h2>
          <p
            style={{
              fontSize: ".85rem",
              color: "var(--text-muted)",
              margin: 0,
            }}
          >
            Manage your assigned ideas below
          </p>
        </div>
        <div className="d-flex gap-2">
          {isAdmin && (
            <button
              className="btn btn-sm btn-outline-warning"
              onClick={handleEod}
            >
              <i className="bi bi-clock-history me-1"></i>Run EOD
            </button>
          )}
          <button
            className="btn btn-sm btn-outline-primary"
            onClick={load}
            disabled={busy}
          >
            <i
              className={`bi bi-arrow-clockwise me-1${busy ? " spin" : ""}`}
            ></i>
            {busy ? "Loading..." : "Refresh"}
          </button>
        </div>
      </div>

      {/* Error */}
      {err && (
        <div
          className="alert alert-danger d-flex align-items-center gap-2 py-2 mb-3"
          style={{ fontSize: ".87rem" }}
        >
          <i className="bi bi-exclamation-triangle-fill"></i>
          <span className="flex-fill">{err}</span>
          <button className="btn btn-sm btn-danger" onClick={load}>
            Retry
          </button>
        </div>
      )}

      {/* Stat cards */}
      <div className="row g-3 mb-4">
        {[
          {
            label: "Total Assigned",
            value: stats.total,
            icon: "bi-briefcase",
            color: "var(--primary)",
          },
          {
            label: "Pending Review",
            value: stats.pending,
            icon: "bi-hourglass-split",
            color: "#f59e0b",
          },
          {
            label: "Accepted",
            value: stats.accepted,
            icon: "bi-check-circle-fill",
            color: "#10b981",
          },
          {
            label: "Rejected",
            value: stats.rejected,
            icon: "bi-x-circle-fill",
            color: "#ef4444",
          },
          {
            label: "Refinement",
            value: stats.refine,
            icon: "bi-arrow-repeat",
            color: "#8b5cf6",
          },
        ].map((s) => (
          <div key={s.label} className="col-6 col-sm-4 col-xl-2">
            <div className="stat-card">
              <div
                className="stat-icon"
                style={{ color: s.color }}
              >
                <i className={`bi ${s.icon}`}></i>
              </div>
              <div className="stat-value" style={{ color: s.color }}>
                {s.value}
              </div>
              <div className="stat-label">{s.label}</div>
            </div>
          </div>
        ))}
      </div>

      {/* Table container */}
      <div className="table-container">
        {/* Filters + search */}
        <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
          <div className="d-flex flex-wrap gap-1 flex-fill">
            {FILTERS.map((f) => (
              <button
                key={f}
                className={`btn btn-sm pill-btn ${
                  filter === f ? "btn-primary" : "btn-outline-primary"
                }`}
                onClick={() => {
                  setFilter(f);
                  setSearch("");
                }}
              >
                {f}
              </button>
            ))}
          </div>
          <div
            className="input-group input-group-sm"
            style={{ maxWidth: 260 }}
          >
            <span className="input-group-text border-end-0 bg-white">
              <i className="bi bi-search text-muted"></i>
            </span>
            <input
              type="text"
              className="form-control border-start-0"
              placeholder="Search..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            {search && (
              <button
                className="btn btn-outline-secondary"
                onClick={() => setSearch("")}
              >
                <i className="bi bi-x"></i>
              </button>
            )}
          </div>
        </div>

        {/* Table */}
        <div className="table-responsive">
          <table
            className="table align-middle mb-0"
            style={{ borderCollapse: "separate", borderSpacing: 0 }}
          >
            <thead>
              <tr>
                {[
                  "Idea ID",
                  "Title",
                  "Employee",
                  "Category",
                  "Stage",
                  "Status",
                  "Review Date",
                ].map((col) => (
                  <th
                    key={col}
                    style={{
                      fontSize: ".78rem",
                      fontWeight: 700,
                      textTransform: "uppercase",
                      letterSpacing: ".4px",
                      color: "var(--text-muted)",
                      borderBottom: "1px solid #edf0f9",
                      paddingBottom: ".65rem",
                    }}
                  >
                    {col}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {busy && !rows.length && (
                <tr>
                  <td
                    colSpan={7}
                    className="text-center py-5 text-muted"
                  >
                    <span className="spinner-border spinner-border-sm me-2 text-primary"></span>
                    Loading ideas...
                  </td>
                </tr>
              )}
              {!busy && filtered.length === 0 && (
                <tr>
                  <td colSpan={7}>
                    <div className="it-empty py-5">
                      <i className="bi bi-inbox fs-2 d-block mb-2"></i>
                      {search
                        ? `No results for "${search}"`
                        : `No ideas with filter "${filter}"`}
                    </div>
                  </td>
                </tr>
              )}
              {filtered.map((r) => (
                <tr
                  key={r.ideaId}
                  className="idea-row"
                  onClick={() => setSelectedId(r.ideaId)}
                  style={{ cursor: "pointer" }}
                >
                  <td>
                    <span
                      className="fw-bold"
                      style={{
                        color: "var(--primary)",
                        fontSize: ".88rem",
                      }}
                    >
                      #{r.ideaId}
                    </span>
                  </td>
                  <td style={{ maxWidth: 230 }}>
                    <span
                      className="fw-semibold"
                      style={{
                        fontSize: ".88rem",
                        color: "var(--text-main)",
                      }}
                    >
                      {r.ideaTitle ?? "—"}
                    </span>
                  </td>
                  <td
                    style={{
                      fontSize: ".88rem",
                      color: "var(--text-main)",
                    }}
                  >
                    {r.employeeName ?? "—"}
                  </td>
                  <td
                    style={{
                      fontSize: ".88rem",
                      color: "var(--text-muted)",
                    }}
                  >
                    {r.categoryName ?? "—"}
                  </td>
                  <td
                    className="text-center"
                    style={{ fontSize: ".88rem" }}
                  >
                    {r.assignmentStage ?? "—"}
                  </td>
                  <td>
                    <span className={statusPill(displayStatus(r))}>
                      {displayStatus(r)}
                    </span>
                  </td>
                  <td
                    style={{
                      fontSize: ".82rem",
                      color: "var(--text-muted)",
                      whiteSpace: "nowrap",
                    }}
                  >
                    {r.assignedDate
                      ? new Date(r.assignedDate).toLocaleDateString(
                          "en-GB",
                          {
                            day: "2-digit",
                            month: "short",
                            year: "numeric",
                          }
                        )
                      : "—"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Count footer */}
        {!busy && filtered.length > 0 && (
          <div
            className="pt-2 pb-1"
            style={{
              fontSize: ".8rem",
              color: "var(--text-muted)",
              borderTop: "1px solid #f4f7fe",
            }}
          >
            Showing {filtered.length}
            {filtered.length !== deduplicated.length
              ? ` of ${deduplicated.length}`
              : ""}{" "}
            idea{filtered.length !== 1 ? "s" : ""}
          </div>
        )}
      </div>
    </div>
  );
};

export default ReviewerDashboard;
