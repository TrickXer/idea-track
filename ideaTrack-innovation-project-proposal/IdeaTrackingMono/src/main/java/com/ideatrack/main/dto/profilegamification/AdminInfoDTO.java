package com.ideatrack.main.dto.profilegamification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Encapsulates admin header info and optional decision metadata for the hierarchy response.
 * Keeps admin-related fields grouped under a single "admin" object in the payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminInfoDTO {
    private Integer adminUserId;
    private String adminName;
    private String adminRole;
    private String adminDept;

    private String adminPhoneNo;
    private String adminEmail;
    private String adminProfileUrl;
    private String adminBio;

    private String decision;              // e.g., "APPROVED" | "REJECTED" | null
    private LocalDateTime decisionAt;     // null if no recorded decision
}