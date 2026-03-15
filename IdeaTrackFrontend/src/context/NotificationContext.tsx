import React, { createContext, useContext, useEffect, useRef, useState } from "react";
import { subscribeToNotificationStream } from "../utils/notificationApi";
import { useAuth } from "../utils/authContext";
import { fetchMyProfile } from "../utils/profileApi";
import type { NotificationResponse, NotificationSSEEvent } from "../utils/types";

/**
 * ✅ SINGLETON NOTIFICATION SERVICE
 * 
 * This context ensures that ONLY ONE SSE connection is created,
 * regardless of how many components subscribe to notifications.
 * 
 * Without this:
 * - NotificationBell creates connection #1
 * - AllNotifications creates connection #2
 * - Backend receives 2 SSE clients for same user (wasteful)
 * 
 * With this:
 * - First component to use hook triggers connection
 * - All components share the same connection
 * - Unsubscribe when last component unmounts
 */

interface NotificationContextType {
  notifications: NotificationResponse[];
  userId: number | null;
  unreadCount: number;
  connectionStatus: "connected" | "disconnected" | "connecting";
  addNotification: (notif: NotificationResponse) => void;
  markNotificationRead: (notifId: number) => void;
  markAllNotificationsRead: () => void;
  clearNotifications: () => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

interface NotificationProviderProps {
  children: React.ReactNode;
}

/**
 * Provider component - wrap your app with this to enable singleton notifications
 */
export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
  const { token } = useAuth();
  const [userId, setUserId] = useState<number | null>(null);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [connectionStatus, setConnectionStatus] = useState<"connected" | "disconnected" | "connecting">("disconnected");
  
  const abortControllerRef = useRef<AbortController | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const maxReconnectAttempts = 10;
  const baseReconnectDelay = 1000;

  // Resolve userId whenever the auth token changes (login / logout)
  useEffect(() => {
    if (!token) {
      // Logged out – immediately tear down SSE connection and cancel any pending reconnect
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
      reconnectAttemptsRef.current = 0;
      // Clear userId so the SSE useEffect also skips reconnection
      setUserId(null);
      setNotifications([]);
      setUnreadCount(0);
      setConnectionStatus("disconnected");
      return;
    }

    const resolveUserId = async () => {
      try {
        const profile = await fetchMyProfile();
        setUserId(profile.userId);
      } catch (err) {
        // 401 can happen if token just expired; log quietly
        console.debug("[NotificationContext] Could not resolve userId:", err);
      }
    };

    resolveUserId();
  }, [token]);

  // Main SSE connection logic (created once per provider)
  useEffect(() => {
    if (userId === null) return;

    const connectToStream = () => {
      setConnectionStatus("connecting");
      try {
        console.log("[NotificationContext] 🔌 Creating singleton SSE connection for userId:", userId);
        
        const controller = subscribeToNotificationStream(
          userId,
          // onEvent callback
          (event) => {
            try {
              // Only process notification events (skip heartbeat, connected, etc.)
              if (event.type !== "notification") {
                console.log(`[NotificationContext] Skipping event type: ${event.type}`);
                return;
              }

              const data = event.data as NotificationSSEEvent;
              // Map SSE event to NotificationResponse
              const mapped: NotificationResponse = {
                notificationId: data.notificationId,
                userId,
                notificationType: data.type,
                notificationTitle: data.title,
                notificationMessage: data.message,
                priority: data.priority,
                notificationStatus: "UNREAD",
                pushed: true,
                metadata: data.metadata,
                createdAt: data.createdAtIso,
                updatedAt: data.createdAtIso,
              };

              console.log("[NotificationContext] ✅ New notification received, broadcasting to all subscribers:", mapped);
              
              // Add notification to global state (all subscribed components see it)
              setNotifications((prev) => {
                // Avoid duplicates by notificationId
                if (prev.some((n) => n.notificationId === mapped.notificationId)) {
                  console.log("[NotificationContext] ℹ️ Notification already exists, skipping duplicate");
                  return prev;
                }
                return [mapped, ...prev];
              });
              
              setUnreadCount((c) => c + 1);
            } catch (err) {
              console.warn("[NotificationContext] Failed to process event:", err);
            }
          },
          // onError callback
          (error) => {
            console.warn(`[NotificationContext] Connection error (attempt ${reconnectAttemptsRef.current + 1}/${maxReconnectAttempts}):`, error);
            setConnectionStatus("disconnected");
            
            // Auto-reconnect with exponential backoff (only if still authenticated)
            if (reconnectAttemptsRef.current < maxReconnectAttempts) {
              reconnectAttemptsRef.current++;
              const delay = baseReconnectDelay * Math.pow(2, reconnectAttemptsRef.current - 1);
              console.log(`[NotificationContext] Reconnecting in ${delay}ms...`);
              reconnectTimeoutRef.current = setTimeout(() => {
                // Guard: do not reconnect if the user has logged out
                if (!abortControllerRef.current && reconnectAttemptsRef.current === 0) {
                  console.log("[NotificationContext] Skipping reconnect — user logged out");
                  return;
                }
                connectToStream();
              }, delay);
            } else {
              console.error("[NotificationContext] Max reconnection attempts reached. Manual refresh required.");
            }
          },
          // onClose callback
          () => {
            console.log("[NotificationContext] Stream closed normally");
            setConnectionStatus("disconnected");
          }
        );
        
        abortControllerRef.current = controller;
        setConnectionStatus("connected");
        reconnectAttemptsRef.current = 0; // Reset on successful connection
      } catch (err) {
        console.error("[NotificationContext] Failed to subscribe:", err);
        setConnectionStatus("disconnected");
        
        // Retry on error (only if still authenticated)
        if (reconnectAttemptsRef.current < maxReconnectAttempts) {
          reconnectAttemptsRef.current++;
          const delay = baseReconnectDelay * Math.pow(2, reconnectAttemptsRef.current - 1);
          reconnectTimeoutRef.current = setTimeout(() => {
            if (!abortControllerRef.current && reconnectAttemptsRef.current === 0) {
              return; // logged out
            }
            connectToStream();
          }, delay);
        }
      }
    };

    connectToStream();

    return () => {
      // Cleanup: abort stream and cancel any pending reconnect
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      if (abortControllerRef.current) {
        console.log("[NotificationContext] 🛑 Aborting SSE connection");
        abortControllerRef.current.abort();
      }
    };
  }, [userId]);

  const addNotification = (notif: NotificationResponse) => {
    setNotifications((prev) => {
      if (prev.some((n) => n.notificationId === notif.notificationId)) {
        return prev;
      }
      return [notif, ...prev];
    });
    setUnreadCount((c) => c + 1);
  };

  const markNotificationRead = (notifId: number) => {
    setNotifications((prev) =>
      prev.map((n) =>
        n.notificationId === notifId
          ? { ...n, notificationStatus: "READ" }
          : n
      )
    );
    setUnreadCount((c) => Math.max(0, c - 1));
  };

  const markAllNotificationsRead = () => {
    setNotifications((prev) =>
      prev.map((n) => ({ ...n, notificationStatus: "READ" }))
    );
    setUnreadCount(0);
  };

  const clearNotifications = () => {
    setNotifications([]);
    setUnreadCount(0);
  };

  return (
    <NotificationContext.Provider
      value={{
        notifications,
        userId,
        unreadCount,
        connectionStatus,
        addNotification,
        markNotificationRead,
        markAllNotificationsRead,
        clearNotifications,
      }}
    >
      {children}
    </NotificationContext.Provider>
  );
};

/**
 * Hook to access the singleton notification service
 * 
 * Usage:
 * ```tsx
 * const { notifications, unreadCount } = useNotifications();
 * ```
 */
export const useNotifications = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error("useNotifications must be used within <NotificationProvider>");
  }
  return context;
};
