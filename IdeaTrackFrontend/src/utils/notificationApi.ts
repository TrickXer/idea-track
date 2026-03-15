import restApi, { BASE_URL } from "./restApi";
import type {
  NotificationCreateRequest,
  BulkNotificationRequest,
  NotificationResponse,
  MarkReadRequest,
  MarkAllReadRequest,
  PageResponse,
  NotificationQueryParams,
} from "./types";

const BASE = "/api/notifications";

/** POST /api/notifications – Create a single notification. */
export async function createNotification(
  data: NotificationCreateRequest
): Promise<NotificationResponse> {
  const response = await restApi.post<NotificationResponse>(BASE, data);
  return response.data;
}

/** POST /api/notifications/bulk – Create multiple notifications. */
export async function createBulkNotifications(
  data: BulkNotificationRequest
): Promise<NotificationResponse[]> {
  const response = await restApi.post<NotificationResponse[]>(
    `${BASE}/bulk`,
    data
  );
  return response.data;
}

/** GET /api/notifications – List notifications with filters & pagination. */
export async function fetchNotifications(
  params: NotificationQueryParams
): Promise<PageResponse<NotificationResponse>> {
  const response = await restApi.get<PageResponse<NotificationResponse>>(
    BASE,
    { params }
  );
  return response.data;
}

/** PATCH /api/notifications/read – Mark specific notifications as READ. */
export async function markNotificationsRead(
  data: MarkReadRequest
): Promise<void> {
  await restApi.patch(`${BASE}/read`, data);
}

/** PATCH /api/notifications/read-all – Mark all notifications for user as READ. */
export async function markAllNotificationsRead(
  data: MarkAllReadRequest
): Promise<void> {
  await restApi.patch(`${BASE}/read-all`, data);
}

/**
 * Interface for SSE stream events
 */
export interface SSEStreamEvent {
  type: string;
  data: unknown;
}

/**
 * Subscribe to real-time SSE notification stream using fetch + ReadableStream.
 * Properly sends JWT token via Authorization header (not query parameter).
 * 
 * @param userId - User ID to subscribe for
 * @param onEvent - Callback function called for each SSE event
 * @param onError - Optional callback for error handling
 * @param onClose - Optional callback when stream closes
 * @returns AbortController to close the stream when needed
 */
export function subscribeToNotificationStream(
  userId: number,
  onEvent: (event: SSEStreamEvent) => void,
  onError?: (error: Error) => void,
  onClose?: () => void
): AbortController {
  const token = localStorage.getItem("jwt-token") ?? "";
  if (!token) {
    const error = new Error("No JWT token found. User must be logged in to subscribe to notifications.");
    onError?.(error);
    throw error;
  }

  const abortController = new AbortController();
  const url = `${BASE_URL}${BASE}/stream?userId=${userId}`;

  const startStream = async () => {
    try {
      console.log(`[SSE] Connecting to notification stream for userId ${userId}`);
      
      const response = await fetch(url, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "text/event-stream",
        },
        signal: abortController.signal,
      });

      if (!response.ok) {
        throw new Error(`SSE stream failed with status ${response.status}: ${response.statusText}`);
      }

      if (!response.body) {
        throw new Error("Response body is null");
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      console.log("[SSE] ✅ Connected to notification stream, waiting for events...");

      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          console.log("[SSE] Stream closed by server");
          // Flush any remaining buffered event
          if (buffer.trim()) {
            console.log("[SSE] Flushing remaining buffer:", buffer);
            parseAndEmitEvent(buffer, onEvent);
          }
          onClose?.();
          break;
        }

        const chunk = decoder.decode(value, { stream: true });
        console.log("[SSE] 📦 Received chunk:", JSON.stringify(chunk));
        
        buffer += chunk;
        console.log("[SSE] 📝 Buffer now:", JSON.stringify(buffer));
        
        // Process complete events (separated by double newline)
        const parts = buffer.split("\n\n");
        console.log("[SSE] Split into", parts.length, 'parts');
        
        // Keep the last potentially incomplete event in buffer
        buffer = parts.pop() ?? "";
        console.log("[SSE] Keeping in buffer:", JSON.stringify(buffer));

        for (let i = 0; i < parts.length; i++) {
          const eventBlock = parts[i];
          console.log(`[SSE] Processing event block ${i}:`, JSON.stringify(eventBlock));
          if (eventBlock.trim()) {
            parseAndEmitEvent(eventBlock, onEvent);
          }
        }
      }
    } catch (error) {
      if (error instanceof Error) {
        if (error.name === "AbortError") {
          console.log("[SSE] Stream aborted by client");
        } else {
          console.error("[SSE] Stream error:", error);
          onError?.(error);
        }
      }
    }
  };

  /**
   * Parse an SSE event block and emit it
   */
  function parseAndEmitEvent(eventBlock: string, emitter: (event: SSEStreamEvent) => void) {
    console.log("[SSE] 🔍 Parsing event block:", JSON.stringify(eventBlock));
    
    const lines = eventBlock.split("\n");
    console.log("[SSE] Split into", lines.length, "lines:", lines.map(l => JSON.stringify(l)));
    
    let eventType = "message";
    let eventId = "";
    let eventData: unknown = null;

    for (const line of lines) {
      const trimmed = line.trim();
      console.log("[SSE] Processing line:", JSON.stringify(trimmed));
      
      if (!trimmed) {
        console.log("[SSE] Empty line, skipping");
        continue;
      }

      if (trimmed.startsWith("event:")) {
        eventType = trimmed.slice(6).trim();
        console.log("[SSE] ✓ Found event type:", eventType);
      } else if (trimmed.startsWith("id:")) {
        eventId = trimmed.slice(3).trim();
        console.log("[SSE] ✓ Found event id:", eventId);
      } else if (trimmed.startsWith("data:")) {
        const dataStr = trimmed.slice(5).trim();
        console.log("[SSE] ✓ Found data string:", JSON.stringify(dataStr));
        // Only try to parse if data looks like JSON (starts with { or [)
        if (dataStr && (dataStr.startsWith("{") || dataStr.startsWith("["))) {
          try {
            eventData = JSON.parse(dataStr);
            console.log("[SSE] ✓ Parsed data:", eventData);
          } catch (parseError) {
            console.warn("[SSE] ❌ Failed to parse event data:", dataStr, parseError);
            eventData = null;
          }
        } else if (dataStr) {
          // For non-JSON data (like "ping"), store as string
          eventData = { data: dataStr };
          console.log("[SSE] ✓ Stored non-JSON data:", eventData);
        }
      }
    }

    console.log(
      "[SSE] 📊 Final event: type=" + eventType + ", id=" + eventId + ", data=",
      eventData
    );
    
    if (eventData !== null) {
      console.log(`[SSE] ✅ Emitting event: type=${eventType}, id=${eventId}`);
      emitter({
        type: eventType,
        data: eventData,
      });
    } else {
      console.log("[SSE] ⚠️ Skipping event because eventData is null");
    }
  }

  // Start the stream asynchronously
  startStream();

  return abortController;
}
