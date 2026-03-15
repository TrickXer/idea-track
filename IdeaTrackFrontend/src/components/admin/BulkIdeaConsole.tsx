import { useState, useEffect, useMemo, useCallback } from "react";
import {
  Zap,
  Wrench,
  Rocket,
  BarChart3,
  Download,
  FolderOpen,
  Search,
  CheckSquare,
  Square,
  User,
  ThumbsUp,
  MessageSquare,
  Tag,
  ImageIcon,
  RefreshCw,
  Layers,
} from "lucide-react";
import {
  fetchAllIdeas,
  bulkIdeaActions,
  exportIdeasByIds,
  exportIdeasByCategory,
  exportAllIdeas,
  downloadCsvBlob,
} from "../../utils/bulkIdeaApi";
import { fetchAllCategories } from "../../utils/categoryApi";
import type {
  BulkActionResult,
  CategoryResponse,
  IdeaResponse,
  IdeaStatus,
} from "../../utils/types";
import "./BulkIdeaConsole.css";

const STATUS_OPTIONS: IdeaStatus[] = [
  "DRAFT",
  "SUBMITTED",
  "UNDERREVIEW",
  "ACCEPTED",
  "REJECTED",
  "REFINE",
  "PROJECTPROPOSAL",
  "APPROVED",
];

const STATUS_COLORS: Record<string, string> = {
  DRAFT: "#6b7280",
  SUBMITTED: "#3b82f6",
  UNDERREVIEW: "#f59e0b",
  ACCEPTED: "#10b981",
  REJECTED: "#ef4444",
  REFINE: "#8b5cf6",
  PROJECTPROPOSAL: "#0ea5e9",
  APPROVED: "#059669",
  PENDING: "#d97706",
};

const BulkIdeaConsole = () => {
  // -- Data state
  const [ideas, setIdeas] = useState<IdeaResponse[]>([]);
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  // -- Sidebar active category ("all" or categoryId string)
  const [activeCategory, setActiveCategory] = useState<string>("all");

  // -- Search within ideas panel
  const [searchQuery, setSearchQuery] = useState("");

  // -- Operations state
  const [opStatus, setOpStatus] = useState<string>("");
  const [opCategoryId, setOpCategoryId] = useState<string>("");
  const [opTag, setOpTag] = useState("");
  const [opClearTag, setOpClearTag] = useState(false);
  const [opFeedback, setOpFeedback] = useState("");
  const [opClearFeedback, setOpClearFeedback] = useState(false);
  const [opThumbnailURL, setOpThumbnailURL] = useState("");
  const [opLoading, setOpLoading] = useState(false);
  const [result, setResult] = useState<BulkActionResult | null>(null);

  // -- Toast
  const [toast, setToast] = useState<string | null>(null);
  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  };

  // -- Load data
  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [ideasRes, cats] = await Promise.all([
        fetchAllIdeas({ size: 500 }),
        fetchAllCategories(),
      ]);
      setIdeas(ideasRes.content);
      setCategories(cats);
    } catch (err) {
      console.error("Failed to load data", err);
      showToast("Failed to load ideas");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // -- Build sidebar category list with counts from ideas
  const sidebarCategories = useMemo(() => {
    const map = new Map<string, { name: string; count: number }>();
    for (const idea of ideas) {
      const key = String(idea.category?.categoryId ?? "none");
      const name = idea.category?.name ?? "Uncategorized";
      const entry = map.get(key);
      if (entry) entry.count++;
      else map.set(key, { name, count: 1 });
    }
    return Array.from(map.entries()).sort((a, b) =>
      a[1].name.localeCompare(b[1].name)
    );
  }, [ideas]);

  // -- Filtered ideas (by sidebar category + search)
  const filteredIdeas = useMemo(() => {
    return ideas.filter((idea) => {
      if (activeCategory !== "all") {
        const catId = String(idea.category?.categoryId ?? "none");
        if (catId !== activeCategory) return false;
      }
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        return (
          idea.title.toLowerCase().includes(q) ||
          (idea.tag ?? "").toLowerCase().includes(q) ||
          idea.author?.displayName?.toLowerCase().includes(q) ||
          String(idea.ideaId).includes(q)
        );
      }
      return true;
    });
  }, [ideas, activeCategory, searchQuery]);

  // -- Selection helpers
  const toggleSelect = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleSelectAll = () => {
    if (selectedIds.size === filteredIdeas.length && filteredIdeas.length > 0) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredIdeas.map((i) => i.ideaId)));
    }
  };

  // -- Submit bulk actions
  const handleApply = async () => {
    if (selectedIds.size === 0) {
      showToast("Select at least one idea");
      return;
    }
    const hasOp =
      opStatus || opCategoryId || opTag || opClearTag || opFeedback || opClearFeedback || opThumbnailURL;
    if (!hasOp) {
      showToast("Select at least one operation");
      return;
    }
    if (opTag && opClearTag) {
      showToast("Cannot set tag AND clear tag at the same time");
      return;
    }
    if (opFeedback && opClearFeedback) {
      showToast("Cannot set feedback AND clear feedback at the same time");
      return;
    }

    setOpLoading(true);
    try {
      const payload = {
        ideaIds: Array.from(selectedIds),
        ...(opStatus && { ideaStatus: opStatus as IdeaStatus }),
        ...(opCategoryId && { categoryId: Number(opCategoryId) }),
        ...(opTag && { tag: opTag }),
        ...(opClearTag && { clearTag: true }),
        ...(opFeedback && { reviewerFeedback: opFeedback }),
        ...(opClearFeedback && { clearReviewerFeedback: true }),
        ...(opThumbnailURL && { thumbnailURL: opThumbnailURL }),
      };
      const res = await bulkIdeaActions(payload);
      setResult(res);
      showToast("Updated " + res.updatedCount + " of " + res.requestedCount + " ideas");
      const refreshed = await fetchAllIdeas({ size: 500 });
      setIdeas(refreshed.content);
      setSelectedIds(new Set());
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : "Bulk action failed";
      showToast(msg);
    } finally {
      setOpLoading(false);
    }
  };

  // -- Export handler (smart: selected → category → all)
  const handleExport = async () => {
    try {
      let blob: Blob;
      let filename: string;

      if (selectedIds.size > 0) {
        blob = await exportIdeasByIds({ ideaIds: Array.from(selectedIds) });
        filename = "ideas_selected_" + Date.now() + ".csv";
      } else if (activeCategory !== "all") {
        blob = await exportIdeasByCategory(Number(activeCategory));
        filename = "ideas_cat_" + activeCategory + "_" + Date.now() + ".csv";
      } else {
        blob = await exportAllIdeas();
        filename = "ideas_all_" + Date.now() + ".csv";
      }

      downloadCsvBlob(blob, filename);
      showToast("Export downloaded");
    } catch {
      showToast("Export failed");
    }
  };

  // -- Render
  const allSelected = filteredIdeas.length > 0 && selectedIds.size === filteredIdeas.length;
  const activeCatLabel =
    activeCategory === "all"
      ? "All Ideas"
      : sidebarCategories.find(([k]) => k === activeCategory)?.[1].name ?? "Ideas";

  return (
    <div className="bulk-page">
      {/* HEADER */}
      <div className="bulk-console-header">
        <div>
          <h2 className="d-flex align-items-center gap-2 mb-1">
            <Zap size={24} /> Bulk Idea Console
          </h2>
          <p className="text-muted mb-0" style={{ fontSize: 14 }}>
            Browse categories, select ideas, and apply bulk operations
          </p>
        </div>
        <button
          className="btn btn-outline-secondary d-flex align-items-center gap-1"
          onClick={loadData}
          disabled={loading}
        >
          <RefreshCw size={16} className={loading ? "spin-icon" : ""} /> Refresh
        </button>
      </div>

      {/* MAIN LAYOUT: sidebar + content */}
      <div className="bulk-layout">
        {/* LEFT SIDEBAR */}
        <div className="bulk-sidebar">
          <div className="sidebar-title">Categories</div>

          {/* All Ideas */}
          <button
            className={"sidebar-item" + (activeCategory === "all" ? " active" : "")}
            onClick={() => { setActiveCategory("all"); setSearchQuery(""); }}
          >
            <Layers size={16} />
            All Ideas
            <span className="sidebar-item-count">{ideas.length}</span>
          </button>

          {/* Each category */}
          {sidebarCategories.map(([key, { name, count }]) => (
            <button
              key={key}
              className={"sidebar-item" + (activeCategory === key ? " active" : "")}
              onClick={() => { setActiveCategory(key); setSearchQuery(""); }}
            >
              <FolderOpen size={16} />
              {name}
              <span className="sidebar-item-count">{count}</span>
            </button>
          ))}
        </div>

        {/* RIGHT CONTENT */}
        <div className="bulk-content">
          {/* Toolbar */}
          <div className="bulk-toolbar">
            <div className="bulk-search-box">
              <Search size={14} className="search-icon" />
              <input
                type="text"
                className="form-control form-control-sm"
                placeholder={"Search in " + activeCatLabel + "..."}
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            <button
              type="button"
              className="btn btn-sm btn-outline-primary d-flex align-items-center gap-1"
              onClick={toggleSelectAll}
            >
              {allSelected ? <CheckSquare size={14} /> : <Square size={14} />}
              {allSelected ? "Deselect" : "Select All"}
            </button>
            <button
              type="button"
              className="btn btn-sm btn-outline-success d-flex align-items-center gap-1 export-btn-toolbar"
              onClick={handleExport}
              title={
                selectedIds.size > 0
                  ? "Export " + selectedIds.size + " selected ideas"
                  : activeCategory !== "all"
                    ? "Export all ideas in " + activeCatLabel
                    : "Export all ideas"
              }
            >
              <Download size={14} />
              {selectedIds.size > 0
                ? "Export (" + selectedIds.size + ")"
                : activeCategory !== "all"
                  ? "Export Category"
                  : "Export All"}
            </button>
            <span className="bulk-toolbar-info">
              {filteredIdeas.length} ideas &middot; <strong>{selectedIds.size}</strong> selected
            </span>
          </div>

          {/* Ideas area */}
          <div className="idea-cards-area">
            {loading ? (
              <div className="bulk-empty">
                <div className="spinner-border text-primary mb-2" role="status" />
                <span>Loading ideas...</span>
              </div>
            ) : filteredIdeas.length === 0 ? (
              <div className="bulk-empty">
                <FolderOpen size={40} strokeWidth={1.2} />
                <p className="mt-2 mb-0">No ideas in this category</p>
              </div>
            ) : (
              <div className="idea-cards-grid">
                {filteredIdeas.map((idea) => {
                  const isSelected = selectedIds.has(idea.ideaId);
                  return (
                    <div
                      key={idea.ideaId}
                      className={"idea-card" + (isSelected ? " selected" : "")}
                      onClick={() => toggleSelect(idea.ideaId)}
                    >
                      <div className="idea-card-top">
                        <div className="idea-card-check">
                          {isSelected ? (
                            <CheckSquare size={16} className="text-primary" />
                          ) : (
                            <Square size={16} className="text-muted" />
                          )}
                        </div>
                        <span className="idea-card-id">#{idea.ideaId}</span>
                        <span
                          className="idea-status-badge"
                          style={{ backgroundColor: STATUS_COLORS[idea.ideaStatus] ?? "#6b7280" }}
                        >
                          {idea.ideaStatus}
                        </span>
                      </div>
                      <h6 className="idea-card-title">{idea.title}</h6>
                      {idea.description && (
                        <p className="idea-card-desc">
                          {idea.description.length > 90
                            ? idea.description.slice(0, 90) + "\u2026"
                            : idea.description}
                        </p>
                      )}
                      <div className="idea-card-meta">
                        <span className="d-flex align-items-center gap-1">
                          <User size={11} />
                          {idea.author?.displayName ?? "Unknown"}
                        </span>
                        {idea.tag && (
                          <span className="idea-tag-chip">
                            <Tag size={9} /> {idea.tag}
                          </span>
                        )}
                      </div>
                      <div className="idea-card-footer">
                        <span className="d-flex align-items-center gap-1">
                          <ThumbsUp size={11} />
                          {idea.votes?.upvotes ?? 0}
                        </span>
                        <span className="d-flex align-items-center gap-1">
                          <MessageSquare size={11} />
                          {idea.commentsCount ?? 0}
                        </span>
                        <span className="idea-card-date">
                          {idea.createdAt ? new Date(idea.createdAt).toLocaleDateString() : ""}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* BOTTOM SECTION: operations + export */}
      <div className="bulk-bottom">
        {/* OPERATIONS PANEL */}
        {selectedIds.size > 0 && (
          <div className="bulk-operations-panel card">
            <div className="card-body">
              <h5 className="d-flex align-items-center gap-2 mb-3">
                <Wrench size={16} /> Bulk Operations
                <span className="badge bg-primary ms-auto">{selectedIds.size} ideas selected</span>
              </h5>
              <div className="op-grid">
                <div>
                  <label className="form-label">Change Status</label>
                  <select className="form-select" value={opStatus} onChange={(e) => setOpStatus(e.target.value)}>
                    <option value="">-- no change --</option>
                    {STATUS_OPTIONS.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label">Move to Category</label>
                  <select className="form-select" value={opCategoryId} onChange={(e) => setOpCategoryId(e.target.value)}>
                    <option value="">-- no change --</option>
                    {categories.map((c) => (
                      <option key={c.categoryId} value={c.categoryId}>
                        {c.name} ({c.department?.deptName})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="form-label d-flex align-items-center gap-1"><Tag size={14} /> Set Tag</label>
                  <input className="form-control" placeholder="Tag value" value={opTag} onChange={(e) => setOpTag(e.target.value)} />
                  <div className="form-check mt-1">
                    <input className="form-check-input" type="checkbox" id="clearTag" checked={opClearTag} onChange={(e) => setOpClearTag(e.target.checked)} />
                    <label className="form-check-label" htmlFor="clearTag">Clear existing tag instead</label>
                  </div>
                </div>
                <div>
                  <label className="form-label">Reviewer Feedback</label>
                  <input className="form-control" placeholder="Feedback text" value={opFeedback} onChange={(e) => setOpFeedback(e.target.value)} />
                  <div className="form-check mt-1">
                    <input className="form-check-input" type="checkbox" id="clearFeedback" checked={opClearFeedback} onChange={(e) => setOpClearFeedback(e.target.checked)} />
                    <label className="form-check-label" htmlFor="clearFeedback">Clear feedback instead</label>
                  </div>
                </div>
                <div>
                  <label className="form-label d-flex align-items-center gap-1"><ImageIcon size={14} /> Thumbnail URL</label>
                  <input className="form-control" placeholder="https://..." value={opThumbnailURL} onChange={(e) => setOpThumbnailURL(e.target.value)} />
                </div>
              </div>
              <button
                className="btn btn-primary btn-lg mt-3 d-flex align-items-center gap-2"
                onClick={handleApply}
                disabled={opLoading}
              >
                {opLoading ? (
                  <><span className="spinner-border spinner-border-sm" /> Processing...</>
                ) : (
                  <><Rocket size={18} /> Apply to {selectedIds.size} Ideas</>
                )}
              </button>
            </div>
          </div>
        )}

        {/* RESULT */}
        {result && (
          <div className="bulk-result card">
            <div className="card-body">
              <h5 className="mb-3 d-flex align-items-center gap-2"><BarChart3 size={18} /> Operation Result</h5>
              <div className="result-metrics">
                <div className="result-metric info"><span className="number">{result.requestedCount}</span><span className="label">Requested</span></div>
                <div className="result-metric info"><span className="number">{result.foundCount}</span><span className="label">Found</span></div>
                <div className="result-metric success"><span className="number">{result.updatedCount}</span><span className="label">Updated</span></div>
                <div className="result-metric warning"><span className="number">{result.notFoundIds.length}</span><span className="label">Not Found</span></div>
              </div>
              {result.notFoundIds.length > 0 && (
                <div className="alert alert-warning py-2" style={{ fontSize: 13 }}>
                  <strong>Not found IDs:</strong> {result.notFoundIds.join(", ")}
                </div>
              )}
              {result.warnings.length > 0 && (
                <div className="alert alert-info py-2" style={{ fontSize: 13 }}>
                  <strong>Warnings:</strong>
                  <ul className="mb-0 mt-1">{result.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>
                </div>
              )}
            </div>
          </div>
        )}


      </div>

      {/* Toast */}
      <div className={"bulk-toast" + (toast ? " show" : "")}>{toast}</div>
    </div>
  );
};

export default BulkIdeaConsole;
