package com.ideatrack.main.dto.profilegamification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Reviewer node shown in the idea hierarchy:
 * - Identity (reviewerId/name/role/department)
 * - Public info (phoneNo, email, profileUrl, bio)
 * - Assignment facts (stage, feedback)
 * - Latest decision (ACCEPTED / REJECTED / PENDING) + decisionAt
 * - Auditing (createdAt)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HierarchyNodeDTO {

    // AssignedReviewerToIdea identifiers
    private Integer id;

    // Reviewer identity
    private Integer reviewerId;
    private String reviewerName;
    private String role;
    private String department;

    // Publicly disclosable reviewer info
    private String phoneNo;
    private String email;       // common, standardized field name
    private String profileUrl;
    private String bio;

    // Assignment facts
    private int stage;
    private String feedback;

    // Decision (DB-derived via latest review note)
    private String decision;          // "ACCEPTED" | "REJECTED" | "PENDING"
    private LocalDateTime decisionAt; // timestamp of that decision

    // Auditing
    private LocalDateTime createdAt;
}