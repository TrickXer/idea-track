package com.ideatrack.main.dto.notification;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCreateRequest {

    @NotNull
    private Integer userId;                  // target receiver

    @NotBlank
    private String notificationType;         // free-form: IDEA_STATUS, COMMENT, SYSTEM, etc.

    @NotBlank
    private String notificationTitle;

    @NotBlank
    private String notificationMessage;

    @NotBlank
    private String priority;                 // e.g., LOW | MEDIUM | HIGH (string-based)

    @Size(max = 4000)
    private String metadata;                 // JSON string: { "redirectTo": "...", "triggeredBy": { ... }, "context": { ... } }
}