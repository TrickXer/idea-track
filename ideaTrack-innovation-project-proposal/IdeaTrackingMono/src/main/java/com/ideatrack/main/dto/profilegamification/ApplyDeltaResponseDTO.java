package com.ideatrack.main.dto.profilegamification;
import lombok.*;

/**
 * Compact response for XP application requests.
 * type: "IDEA_STATUS" or "ACTIVITY"
 * reason: typically the enum name used (e.g., "APPROVED", "COMMENT")
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyDeltaResponseDTO {
    Integer userId;
    Integer delta;
    Integer totalXP;
    String type;   // "IDEA_STATUS" | "ACTIVITY"
    String reason; // e.g., APPROVED / COMMENT (enum name)
}