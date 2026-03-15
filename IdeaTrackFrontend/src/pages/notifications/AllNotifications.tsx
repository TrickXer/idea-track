import { useState, useEffect, useCallback, useRef } from "react";
import {
  CircleDot,
  ChevronLeft,
  ChevronRight,
  PartyPopper,
} from "lucide-react";
import {
  fetchNotifications,
  markNotificationsRead,
  markAllNotificationsRead,
} from "../../utils/notificationApi";
import { useNotifications } from "../../context/NotificationContext";
import NotificationItem from "../../components/notifications/NotificationItem";
import type { NotificationResponse } from "../../utils/types";
import "./AllNotifications.css";

type FilterType = "ALL" | "UNREAD" | "READ" | "HIGH" | "MEDIUM" | "LOW";

const FILTERS: { label: string; value: FilterType; color?: string }[] = [
  { label: "All Notifications", value: "ALL" },
  { label: "Unread", value: "UNREAD" },
  { label: "Read", value: "READ" },
  { label: "High Priority", value: "HIGH", color: "#ef4444" },
  { label: "Medium Priority", value: "MEDIUM", color: "#f59e0b" },
  { label: "Low Priority", value: "LOW", color: "#10b981" },
];

const PAGE_SIZE = 10;

const AllNotifications = () => {
  // ✅ Use singleton notification context (only ONE SSE connection across app)
  const {
    notifications: contextNotifications,
    userId,
    unreadCount,
    markNotificationRead: contextMarkRead,
    markAllNotificationsRead: contextMarkAllRead,
  } = useNotifications();

  const [notifications, setNotifications] = useState<NotificationResponse[]>(
    []
  );
  const [filter, setFilter] = useState<FilterType>("ALL");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  
  // Track context notifications length to detect new arrivals
  const prevContextLengthRef = useRef(0);

  // ── Fetch initial page of notifications ────────────────────
  const loadPage = useCallback(async () => {
    if (userId === null) return;
    setLoading(true);
    try {
      // Determine status filter
      let statusParam: "UNREAD" | "READ" | "ALL" = "ALL";
      if (filter === "UNREAD") statusParam = "UNREAD";
      else if (filter === "READ") statusParam = "READ";

      const res = await fetchNotifications({
        userId,
        status: statusParam,
        page,
        size: PAGE_SIZE,
        sort: "createdAt,desc",
      });

      let data = res.content;

      // Client-side priority filter
      if (filter === "HIGH" || filter === "MEDIUM" || filter === "LOW") {
        data = data.filter((n) => n.priority === filter);
      }

      setNotifications(data);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch (err) {
      console.error("Failed to load notifications", err);
    } finally {
      setLoading(false);
    }
  }, [userId, filter, page]);

  // Load initial data
  useEffect(() => {
    loadPage();
  }, [loadPage]);

  // Reset page on filter change
  useEffect(() => {
    setPage(0);
  }, [filter]);

  // ── Real-time updates from singleton context (no SSE subscription here) ──
  // When new notifications arrive, reload current page and update total count
  useEffect(() => {
    // Always reload from backend to get proper paginated data
    // This ensures we get both historical and new notifications in correct order
    const contextLength = contextNotifications.length;
    const previousLength = prevContextLengthRef.current;
    const newNotificationsCount = contextLength - previousLength;
    
    // Update total if new notifications arrived
    if (newNotificationsCount > 0) {
      setTotalElements((prev) => prev + newNotificationsCount);
      // Reload current page from backend to include new notifications
      loadPage();
    }
    
    prevContextLengthRef.current = contextLength;
  }, [contextNotifications, loadPage]);

  // ── Sync read status changes across all pages ──────────────
  // When a notification is marked as read in context, update it on current page too
  useEffect(() => {
    setNotifications((prev) =>
      prev.map((n) => {
        const updatedInContext = contextNotifications.find(
          (c) => c.notificationId === n.notificationId
        );
        if (updatedInContext && updatedInContext.notificationStatus !== n.notificationStatus) {
          return { ...n, notificationStatus: updatedInContext.notificationStatus };
        }
        return n;
      })
    );
  }, [contextNotifications]);

  // ── Mark read handlers (update DB + context) ──────────────
  const handleMarkRead = async (id: number) => {
    if (userId === null) return;
    try {
      await markNotificationsRead({ userId, notificationIds: [id] });
      // ✅ Update context immediately so all components see the change
      contextMarkRead(id);
    } catch {
      console.error("Failed to mark read");
    }
  };

  const handleMarkAllRead = async () => {
    if (userId === null) return;
    try {
      await markAllNotificationsRead({ userId });
      // ✅ Update context immediately so all components see the change
      contextMarkAllRead();
    } catch {
      console.error("Failed to mark all read");
    }
  };

  // ── Helpers ────────────────────────────────────────────────
  const timeAgo = (isoStr: string): string => {
    const diff = Date.now() - new Date(isoStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return "Just now";
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    if (days < 7) return `${days}d ago`;
    return new Date(isoStr).toLocaleDateString();
  };

  // ── Unread count (from context – global count) ──────────────
  // Don't calculate from filtered notifications, use context's actual count
  const displayUnreadCount = unreadCount; // Use context value directly

  // ── Render ─────────────────────────────────────────────────
  return (
    <div className="all-notif-page">
      <div className="all-notif-layout">
        {/* ── Sidebar ────────────────────────────────────────── */}
        <aside className="notif-sidebar">
          <h2>Activity</h2>
          <div className="notif-filter-group">
            {FILTERS.map((f) => (
              <button
                key={f.value}
                className={`notif-filter-btn ${filter === f.value ? "active" : ""}`}
                onClick={() => setFilter(f.value)}
              >
                {f.color && <CircleDot size={14} color={f.color} style={{ marginRight: 6, flexShrink: 0 }} />}
                {f.label}
              </button>
            ))}
          </div>
        </aside>

        {/* ── Main list ──────────────────────────────────────── */}
        <main>
          <div className="notif-list-card">
            {/* Toolbar */}
            <div className="notif-toolbar">
              <span className="notif-toolbar-left">
                {totalElements} notification{totalElements !== 1 && "s"}
                {displayUnreadCount > 0 && ` · ${displayUnreadCount} unread`}
              </span>
              {displayUnreadCount > 0 && (
                <button onClick={handleMarkAllRead}>
                  Mark all as read
                </button>
              )}
            </div>

            {loading ? (
              <div className="text-center py-5">
                <div className="spinner-border text-primary" role="status" />
              </div>
            ) : notifications.length === 0 ? (
              <div className="notif-empty-page">
                <PartyPopper size={32} />
                <p>No notifications to show</p>
              </div>
            ) : (
              notifications.map((n) => (
                <NotificationItem
                  key={n.notificationId}
                  notification={n}
                  variant="full"
                  onMarkRead={handleMarkRead}
                  timeAgo={timeAgo}
                />
              ))
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="notif-pagination">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="d-flex align-items-center gap-1"
              >
                <ChevronLeft size={16} /> Previous
              </button>
              <span className="page-info">
                Page {page + 1} of {totalPages}
              </span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="d-flex align-items-center gap-1"
              >
                Next <ChevronRight size={16} />
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default AllNotifications;
