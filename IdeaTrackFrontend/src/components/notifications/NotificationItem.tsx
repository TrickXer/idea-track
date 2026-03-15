import { useNavigate } from "react-router-dom";
import {
  Bell,
  CheckCircle,
  AlertCircle,
  MessageSquare,
  Users,
  Clock,
  Award,
  TrendingUp,
  ThumbsUp,
  Star,
  ArrowUpCircle,
  FileText,
  Eye,
} from "lucide-react";
import type { NotificationResponse } from "../../utils/types";
import { parseNotificationMetadata } from "../../utils/types";
import "./NotificationItem.css";

interface NotificationItemProps {
  notification: NotificationResponse;
  variant?: "compact" | "full"; // compact for bell, full for all notifications page
  onMarkRead?: (notifId: number) => void;
  timeAgo: (isoStr: string) => string;
}

const NotificationItem = ({
  notification: n,
  variant = "full",
  onMarkRead,
  timeAgo,
}: NotificationItemProps) => {
  const navigate = useNavigate();
  const meta = parseNotificationMetadata(n.metadata);

  // ── Icon resolution: prefer context.action from metadata, fall back to notificationType ──
  const getIcon = () => {
    const action = meta?.context?.action;
    const iconProps = { size: 16, strokeWidth: 2 };

    // Action-first mapping (most specific)
    switch (action) {
      case "IDEA_APPROVED":
      case "PROPOSAL_APPROVED":
        return <CheckCircle {...iconProps} />;
      case "IDEA_REJECTED":
      case "PROPOSAL_REJECTED":
        return <AlertCircle {...iconProps} />;
      case "IDEA_REFINE":
      case "FEEDBACK_POSTED":
      case "REVIEWER_DISCUSSION":
        return <MessageSquare {...iconProps} />;
      case "IDEA_SUBMITTED":
      case "PROPOSAL_SUBMITTED":
        return <TrendingUp {...iconProps} />;
      case "IDEA_STAGE_ADVANCED":
        return <ArrowUpCircle {...iconProps} />;
      case "COMMENT_ADDED":
      case "MENTION":
        return <MessageSquare {...iconProps} />;
      case "VOTE_CAST":
        return <ThumbsUp {...iconProps} />;
      case "REVIEWER_ASSIGNED":
        return <Eye {...iconProps} />;
      case "REVIEWER_SLA_EXPIRED":
      case "DEADLINE_REMINDER":
        return <Clock {...iconProps} />;
      case "BADGE_EARNED":
      case "LEVEL_UP":
        return <Award {...iconProps} />;
      case "COLLABORATION_INVITE":
        return <Users {...iconProps} />;
      default:
        break;
    }

    // Fallback to notificationType
    switch (n.notificationType) {
      case "REVIEW_ASSIGNMENT":
        return <CheckCircle {...iconProps} />;
      case "APPROVAL_STATUS":
        return <CheckCircle {...iconProps} />;
      case "REJECTION_NOTICE":
        return <AlertCircle {...iconProps} />;
      case "FEEDBACK_RECEIVED":
        return <MessageSquare {...iconProps} />;
      case "COLLABORATION_INVITE":
        return <Users {...iconProps} />;
      case "COMMENT_MENTION":
        return <MessageSquare {...iconProps} />;
      case "DEADLINE_REMINDER":
        return <Clock {...iconProps} />;
      case "ACHIEVEMENT_UNLOCKED":
        return <Award {...iconProps} />;
      case "IDEA_SUBMISSION":
        return <TrendingUp {...iconProps} />;
      case "PROPOSAL_STATUS":
        return <FileText {...iconProps} />;
      default:
        return <Bell {...iconProps} />;
    }
  };

  /** Human-readable action label derived from context.action */
  const getActionLabel = (): string | null => {
    const action = meta?.context?.action;
    if (!action) return null;
    const labels: Record<string, string> = {
      IDEA_SUBMITTED: "Idea Submitted",
      IDEA_APPROVED: "Idea Accepted",
      IDEA_REJECTED: "Idea Rejected",
      IDEA_REFINE: "Needs Refinement",
      IDEA_STAGE_ADVANCED: "Stage Advanced",
      COMMENT_ADDED: "New Comment",
      VOTE_CAST: "Voted",
      FEEDBACK_POSTED: "Feedback Posted",
      REVIEWER_ASSIGNED: "Assigned for Review",
      REVIEWER_DISCUSSION: "Reviewer Note",
      REVIEWER_SLA_EXPIRED: "SLA Expired",
      PROPOSAL_SUBMITTED: "Proposal Submitted",
      PROPOSAL_APPROVED: "Proposal Approved",
      PROPOSAL_REJECTED: "Proposal Rejected",
      BADGE_EARNED: "Badge Earned",
      LEVEL_UP: "Level Up",
      DEADLINE_REMINDER: "Deadline Reminder",
      COLLABORATION_INVITE: "Collaboration Invite",
      MENTION: "Mentioned You",
    };
    return labels[action] ?? null;
  };

  const priorityClass = (p: string) =>
    p === "HIGH" ? "high" : p === "MEDIUM" ? "medium" : "low";

  const handleClick = () => {
    if (n.notificationStatus === "UNREAD" && onMarkRead) {
      onMarkRead(n.notificationId);
    }
    // Navigate using metadata.redirectTo if available
    if (meta?.redirectTo) {
      navigate(meta.redirectTo);
    }
  };

  const triggeredByLabel = meta?.triggeredBy?.name
    ? meta.triggeredBy.name !== "System"
      ? `by ${meta.triggeredBy.name}`
      : null
    : null;

  const actionLabel = getActionLabel();

  if (variant === "compact") {
    // Bell dropdown version
    return (
      <div
        className={`notif-panel-item ${n.notificationStatus === "UNREAD" ? "unread" : ""} ${meta?.redirectTo ? "clickable" : ""}`}
        onClick={handleClick}
        role="button"
        tabIndex={0}
        title={meta?.redirectTo ? "Click to view" : undefined}
      >
        <div className="notif-type-icon">
          {getIcon()}
        </div>
        <div className="notif-item-content">
          <div className="notif-item-title">
            {n.notificationTitle}
            {actionLabel && (
              <span className="notif-action-label">{actionLabel}</span>
            )}
          </div>
          <div className="notif-item-msg">{n.notificationMessage}</div>
          <div className="notif-item-footer">
            <span className={`priority-badge ${priorityClass(n.priority)}`}>
              {n.priority}
            </span>
            {triggeredByLabel && (
              <span className="notif-triggered-by">{triggeredByLabel}</span>
            )}
            <span className="notif-item-time">{timeAgo(n.createdAt)}</span>
          </div>
        </div>
        {meta?.redirectTo && (
          <div className="notif-redirect-arrow" aria-hidden="true">›</div>
        )}
      </div>
    );
  }

  // Full page version
  return (
    <div
      className={`notif-row ${n.notificationStatus === "UNREAD" ? "unread" : ""} ${meta?.redirectTo ? "clickable" : ""}`}
      onClick={handleClick}
      role="button"
      tabIndex={0}
      title={meta?.redirectTo ? "Click to navigate" : undefined}
    >
      {n.notificationStatus === "UNREAD" ? (
        <div className="notif-unread-dot" />
      ) : (
        <div className="notif-dot-spacer" />
      )}
      <div className="notif-type-icon">
        {getIcon()}
      </div>
      <div className="notif-row-body">
        <div className="notif-row-title">
          {n.notificationTitle}
          {actionLabel && (
            <span className="notif-action-label">{actionLabel}</span>
          )}
        </div>
        <div className="notif-row-msg">{n.notificationMessage}</div>
        {/* Rich context: show idea title / badge name from metadata */}
        {meta?.context && (meta.context.ideaTitle || meta.context.badgeName || meta.context.level) && (
          <div className="notif-context-hint">
            {meta.context.ideaTitle && (
              <span className="notif-context-chip">
                <Star size={11} /> {meta.context.ideaTitle}
                {meta.context.stage !== undefined && ` · Stage ${meta.context.stage}`}
              </span>
            )}
            {meta.context.badgeName && (
              <span className="notif-context-chip">
                <Award size={11} /> {meta.context.badgeName}
              </span>
            )}
            {meta.context.level && !meta.context.badgeName && (
              <span className="notif-context-chip">
                <TrendingUp size={11} /> Level: {meta.context.level}
              </span>
            )}
          </div>
        )}
        <div className="notif-row-footer">
          <span className={`priority-badge ${priorityClass(n.priority)}`}>
            {n.priority}
          </span>
          {triggeredByLabel && (
            <span className="notif-triggered-by">{triggeredByLabel}</span>
          )}
          <span className="notif-row-time">{timeAgo(n.createdAt)}</span>
          {meta?.redirectTo && (
            <span className="notif-row-link">View →</span>
          )}
        </div>
      </div>
    </div>
  );
};

export default NotificationItem;

