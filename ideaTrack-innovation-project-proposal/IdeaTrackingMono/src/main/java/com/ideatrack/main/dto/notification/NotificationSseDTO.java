package com.ideatrack.main.dto.notification;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSseDTO {
    private Integer notificationId;
    private String type;
    private String title;
    private String message;
    private String priority;
    private String metadata;      // JSON string (redirectTo, triggeredBy, etc.)
    private String createdAtIso;  // ISO-8601 string for SSE convenience
}