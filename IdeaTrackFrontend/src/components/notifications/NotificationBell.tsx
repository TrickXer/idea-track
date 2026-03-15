import { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Bell, PartyPopper } from "lucide-react";
import {
  markNotificationsRead,
  markAllNotificationsRead,
} from "../../utils/notificationApi";
import { useNotifications } from "../../context/NotificationContext";
import NotificationItem from "./NotificationItem";
import "./NotificationBell.css";

const NotificationBell = () => {
  const navigate = useNavigate();
  // ✅ Use singleton notification context (only ONE SSE connection across app)
  const {
    notifications: allNotifications,
    userId,
    unreadCount,
    markNotificationRead: contextMarkRead,
    markAllNotificationsRead: contextMarkAllRead,
  } = useNotifications();

  const [open, setOpen] = useState(false);
  const panelRef = useRef<HTMLDivElement>(null);

  // Show only 10 most recent for bell dropdown (sorted by createdAt descending to match backend order)
  const notifications = [...allNotifications]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 10);

  // ── Close panel on outside click ──────────────────────────
  useEffect(() => {
    if (!open) return;
    
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Node;
      // Check if click is outside the panel AND not on the bell icon
      if (panelRef.current && !panelRef.current.contains(target)) {
        setOpen(false);
      }
    };
    
    // Use 'click' instead of 'mousedown' to allow onClick to fire first
    document.addEventListener("click", handleClickOutside);
    return () => document.removeEventListener("click", handleClickOutside);
  }, [open]);

  // ── Mark single as read (update DB + context) ──────────────
  const handleMarkRead = async (notifId: number) => {
    if (userId === null) return;
    try {
      await markNotificationsRead({ userId, notificationIds: [notifId] });
      // ✅ Update context immediately so all components see the change
      contextMarkRead(notifId);
    } catch {
      console.error("Failed to mark notification read");
    }
  };

  // ── Mark all read (update DB + context) ──────────────────
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

  // ── Time formatting ────────────────────────────────────────
  const timeAgo = (isoStr: string): string => {
    const diff = Date.now() - new Date(isoStr).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return "Just now";
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
  };

  return (
    <div
      className="bell-wrapper"
      ref={panelRef}
      onClick={(e) => {
        e.stopPropagation();
        setOpen(!open);
      }}
      role="button"
      tabIndex={0}
      aria-label="Notifications"
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          setOpen(!open);
        }
      }}
    >
      <span className="bell-icon">
        <Bell size={22} />
      </span>
      {unreadCount > 0 && (
        <span className="bell-badge">
          {unreadCount > 99 ? "99+" : unreadCount}
        </span>
      )}

      {open && (
        <div className="notif-panel" onClick={(e) => e.stopPropagation()}>
          <div className="notif-panel-header">
            <h6>Notifications</h6>
            {unreadCount > 0 && (
              <button className="mark-all-btn" onClick={handleMarkAllRead}>
                Mark all as read
              </button>
            )}
          </div>

          <div className="notif-panel-list">
            {notifications.length === 0 ? (
              <div className="notif-empty"><PartyPopper size={20} style={{ marginRight: 6 }} /> You're all caught up!</div>
            ) : (
              notifications.map((n) => (
                <NotificationItem
                  key={n.notificationId}
                  notification={n}
                  variant="compact"
                  onMarkRead={(id) => {
                    handleMarkRead(id);
                    setOpen(false); // close the bell panel on click-through
                  }}
                  timeAgo={timeAgo}
                />
              ))
            )}
          </div>

          <div className="notif-panel-footer">
            <button
              onClick={(e) => {
                e.stopPropagation();
                setOpen(false);
                navigate("/notifications");
              }}
            >
              Show All Notifications
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default NotificationBell;
