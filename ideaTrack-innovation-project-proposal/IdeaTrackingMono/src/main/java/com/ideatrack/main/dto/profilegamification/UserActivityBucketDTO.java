package com.ideatrack.main.dto.profilegamification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Compact response bucket for user activities filtered by ActivityType.
 * Example: { "count": 12, "data": [ UserActivityDTO, ... ] }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityBucketDTO {
    private long count;
    private List<UserActivityDTO> data;
}