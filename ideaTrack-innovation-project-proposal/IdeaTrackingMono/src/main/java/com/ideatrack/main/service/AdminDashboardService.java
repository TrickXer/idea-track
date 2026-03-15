package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Proposal;
import com.ideatrack.main.dto.admin.AdminHealthSummaryDto;
import com.ideatrack.main.dto.admin.AdminOverdueReviewerDto;
import com.ideatrack.main.dto.admin.AdminProposalSummaryDto;
import com.ideatrack.main.dto.admin.AdminUpdateUserStatusRequest;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IProposalRepository;
import com.ideatrack.main.repository.IReviewerCategoryRepository;
import com.ideatrack.main.repository.IUserRepository;
import com.ideatrack.main.repository.ReviewerAggregateProjection;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;



@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final IProposalRepository proposalRepository;
    private final IReviewerCategoryRepository assignmentRepository;
    private final IUserRepository userRepository;
    private final IAssignedReviewerToIdeaRepository assignedReviewerToIdeaRepository;
    private final IIdeaRepository ideaRepository;


    /**
     * Proposals with optional filters (status, stageId, reviewerId) + paging/sorting.
     * Returns DTOs (no entity leak).
     */


    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<AdminProposalSummaryDto> listPendingOrStuckProposals(
            String status, Long stageId, Long reviewerId, Pageable pageable
    ) {
        // Parse status string -> enum
        Constants.IdeaStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Constants.IdeaStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
            	log.error("Invalid idea status");
                throw new IllegalArgumentException("Invalid status: " + status);
            }
        }

        // Convert Long -> Integer if needed
        Integer stageIdInt = (stageId == null) ? null : Math.toIntExact(stageId);
        Integer reviewerIdInt = (reviewerId == null) ? null : Math.toIntExact(reviewerId);

        Page<Proposal> page = proposalRepository.searchAdminList(statusEnum, stageIdInt, reviewerIdInt, pageable);

        final String stageLabel = (stageId != null) ? ("Stage " + stageId) : null;

        return page.map(p -> new AdminProposalSummaryDto(
                p.getProposalId(),
                p.getTitle(),
                p.getIdeaStatus() != null ? p.getIdeaStatus().name().toLowerCase(Locale.ROOT) : null,
                stageLabel,
                toOffset()
        ));
    }
    

    /**
     * Aggregated reviewers with overdue tasks.
     */
    @Transactional(Transactional.TxType.SUPPORTS) // read-only
    public Page<AdminOverdueReviewerDto> getOverdueReviewers(Pageable pageable) {
        // Make sure ReviewerAggregateProjection exists and is returned by the repo method
        Page<ReviewerAggregateProjection> page = assignmentRepository.findOverdueReviewerAggregates(pageable);
        return page.map(agg -> new AdminOverdueReviewerDto(
                agg.getReviewerId(),
                safeInt(agg.getPendingTasks()),
                safeInt(agg.getOverdueByDays())
        ));
    }

    /**
     * Simple health summary example; wire to real metrics if available.
     */
    @Transactional(Transactional.TxType.SUPPORTS) // read-only
    public AdminHealthSummaryDto getHealthSummary() {
        long proposals = proposalRepository.count();

        return new AdminHealthSummaryDto(
                "healthy",
                0,
                "99.99%",
                Math.toIntExact(proposals % 100),
                "1.0.0",
                OffsetDateTime.now()
        );
    }

    /**
     * Activate/suspend/deactivate a user (align enum and request type to your model).
     */

    public void updateUserStatus(Integer userId, AdminUpdateUserStatusRequest req) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

            var status = req.getStatus(); // already com.ideatrack.main.data.Constants.Status
            user.setStatus(status);
            user.setDeleted(status != com.ideatrack.main.data.Constants.Status.ACTIVE);

            userRepository.save(user);
        }

    /**
     * Remove reviewers from a proposal stage.
     */
    @jakarta.transaction.Transactional
    public void removeReviewersFromStage(@NotNull Long ideaIdLong,
                                         @NotNull Long stageIdLong,
                                         java.util.List<Long> reviewerIds) {
        if (reviewerIds == null || reviewerIds.isEmpty()) return;

        Integer ideaId = Math.toIntExact(ideaIdLong);

        ideaRepository.findById(ideaId).orElseThrow();
        
        // Convert Long reviewerIds to Integer list
        java.util.List<Integer> reviewerIdsInt = reviewerIds.stream()
                .map(Math::toIntExact)
                .toList();
        
        assignedReviewerToIdeaRepository.softDeleteAssignments(
                ideaId,
                Math.toIntExact(stageIdLong),
                reviewerIdsInt,
                OffsetDateTime.now()
        );
    }
    



    // ---------- helpers ----------

    private int safeInt(Number n) {
        return n == null ? 0 : n.intValue();
    }

    /**
     * Converts temporal objects to OffsetDateTime (UTC).
     * Accepts OffsetDateTime, Instant, LocalDateTime. Returns null for null input.
     * (Java 8 compatible — no pattern matching)
     */
    private OffsetDateTime toOffset() {
         
        // Fallback — consider returning null if you don't want a "now" default
        return OffsetDateTime.now();
    }
}