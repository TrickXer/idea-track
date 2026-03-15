package com.ideatrack.main.dto.profilegamification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public, sanitized profile DTO for external consumption.
 * Includes non-sensitive gamification aggregates (totalXp and level as String),
 * but excludes internal flags, badges, and other detailed metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserProfileDTO {
    private Integer userId;
    private String name;
    private String email;
    private String phoneNo;
    private String bio;
    private String profileUrl;
    private String role;
    private String departmentName;
    private Integer totalXp;
    private String level;
}