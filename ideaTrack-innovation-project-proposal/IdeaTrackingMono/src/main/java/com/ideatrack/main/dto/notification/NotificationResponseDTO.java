package com.ideatrack.main.dto.notification;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {

    private Integer notificationId;
    private Integer userId;

    private String notificationType;
    private String notificationTitle;
    private String notificationMessage;
    private String priority;

    private String notificationStatus;    // UNREAD | READ
    private boolean pushed;               // maps to entity.isPushed

    private String metadata;              // JSON string

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}