package com.ideatrack.main.dto.admin;

import java.time.OffsetDateTime;

import lombok.Builder;


//Done by vibhuti

/**
 * System health snapshot for admin dashboard.
 */
@Builder
public record AdminHealthSummaryDto(
        String database,        // healthy | degraded | down
        Integer queueBacklog,
        String serviceUptime,   // e.g., "99.99%"
        Integer pendingJobs,
        String version,
        OffsetDateTime timestamp
) {}
