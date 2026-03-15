/**
 Author - Pavan
 */
package com.ideatrack.main.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.ideatrack.main.data.AssignedReviewerToIdea;
import com.ideatrack.main.dto.reviewer.ReviewerDashboardDTO;
import com.ideatrack.main.exception.UnauthorizedException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IUserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewerDashboardService {

    private final IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    private final IUserRepository userRepository;

    /**
     * Returns the dashboard for the currently authenticated reviewer.
     * The reviewer ID is resolved securely from the JWT security context —
     * it is NOT taken from the URL to prevent ID tampering.
     */
    public List<ReviewerDashboardDTO> getReviewerDashboard(String filter) {
        Integer reviewerId = getAuthenticatedUserId();
        return getReviewerDashboard(reviewerId, filter);
    }

    /** Internal helper — kept for potential admin/SUPERADMIN use. */
    public List<ReviewerDashboardDTO> getReviewerDashboard(Integer reviewerId, String filter) {
        String normalized = normalize(filter);

        List<AssignedReviewerToIdea> list = reviewerAssignRepo.findDashboardIdeas(
                reviewerId,
                "ALL".equals(normalized) || "UNDERREVIEW".equals(normalized) ? null : normalized
        );

        return list.stream()
                .filter(a -> applyFilter(a, normalized))
                .map(this::toDTO)
                .toList();
    }

    private Integer getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user");
        }
        String username = auth.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found: " + username))
                .getUserId();
    }

    private boolean applyFilter(AssignedReviewerToIdea a, String filter) {
        if ("ALL".equals(filter)) return true;

        // Use the idea's live status as source of truth so that after
        // an owner resubmits a REFINE idea the row moves to UNDERREVIEW.
        String ideaStatus = a.getIdea().getIdeaStatus() != null
                ? a.getIdea().getIdeaStatus().name().toUpperCase()
                : "UNDERREVIEW";

        // Map idea statuses that mean "under review" to UNDERREVIEW filter
        boolean isUnder = "UNDERREVIEW".equals(ideaStatus)
                || "SUBMITTED".equals(ideaStatus);

        if ("UNDERREVIEW".equals(filter)) return isUnder;

        // For ACCEPTED / REJECTED / REFINE: use the idea's actual status
        return ideaStatus.equalsIgnoreCase(filter);
    }

    private String normalize(String f) {
        if (f == null || f.isBlank()) return "ALL";
        String v = f.trim().toUpperCase();
        if ("APPROVED".equals(v) || "APPROVE".equals(v)) v = "ACCEPTED";
        if ("ACCEPT".equals(v)) v = "ACCEPTED";
        if ("REJECT".equals(v)) v = "REJECTED";
        return v;
    }

    private ReviewerDashboardDTO toDTO(AssignedReviewerToIdea a) {
        String displayDecision = (a.getDecision() == null || a.getDecision().isBlank())
                ? "UNDERREVIEW"
                : a.getDecision();

        return ReviewerDashboardDTO.builder()
                .ideaId(a.getIdea().getIdeaId())
                .ideaTitle(a.getIdea().getTitle())
                .employeeName(a.getIdea().getUser() != null ? a.getIdea().getUser().getName() : "Unknown")
                .categoryName(a.getIdea().getCategory() != null ? a.getIdea().getCategory().getName() : "Unknown")
                .assignmentStage(a.getStage())
                .currentIdeaStatus(a.getIdea().getIdeaStatus() != null ? a.getIdea().getIdeaStatus().name() : null)
                .reviewerDecision(displayDecision)
                .assignedDate(a.getCreatedAt())
                .build();
    }
}
