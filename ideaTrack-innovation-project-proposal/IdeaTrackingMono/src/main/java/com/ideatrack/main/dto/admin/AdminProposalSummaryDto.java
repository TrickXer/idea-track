package com.ideatrack.main.dto.admin;

import java.time.OffsetDateTime;

//Done by vibhuti

/**
 * Lightweight view of a proposal for admin listing.
 */
public record AdminProposalSummaryDto(
        Integer proposalId,
        String title,
        String status,          // e.g., pending | stuck | approved | rejected
        String currentStage,
        OffsetDateTime lastUpdatedAt
) {}
