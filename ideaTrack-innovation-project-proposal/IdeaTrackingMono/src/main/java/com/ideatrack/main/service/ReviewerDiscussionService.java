/**
 Author - Pavan
 */
package com.ideatrack.main.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.dto.PagedResponse;
import com.ideatrack.main.dto.reviewer.ReviewerDiscussionDTO;
import com.ideatrack.main.exception.ReviewerBadRequestException;
import com.ideatrack.main.exception.ReviewerForbiddenException;
import com.ideatrack.main.exception.ReviewerNotFoundException;
import com.ideatrack.main.repository.IAssignedReviewerToIdeaRepository;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReviewerDiscussionService {

    private static final String EVENT_FOLLOWUP = "FOLLOWUP";

    private final IIdeaRepository ideaRepo;
    private final IAssignedReviewerToIdeaRepository reviewerAssignRepo;
    private final IUserActivityRepository activityRepo;
    private final IUserRepository userRepo;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;

    public void postDiscussion(Integer ideaId, Integer userId, Integer stageId, String text, Integer replyParent) {
        if (ideaId == null) throw new ReviewerBadRequestException("REV_IDEAID_REQUIRED", "ideaId is required");
        if (userId == null) throw new ReviewerBadRequestException("REV_USER_REQUIRED", "userId is required");
        if (stageId == null) throw new ReviewerBadRequestException("REV_STAGE_REQUIRED", "stageId is required");
        if (text == null || text.isBlank()) throw new ReviewerBadRequestException("REV_TEXT_REQUIRED", "text is required");
        if (text.length() > 2000) throw new ReviewerBadRequestException("REV_TEXT_TOO_LONG", "text must be <= 2000");

        Idea idea = ideaRepo.findById(ideaId)
                .orElseThrow(() -> new ReviewerNotFoundException("REV_IDEA_NOT_FOUND", "Idea not found: " + ideaId));
        if (idea.isDeleted()) throw new ReviewerBadRequestException("REV_IDEA_DELETED", "Idea deleted");

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ReviewerNotFoundException("REV_USER_NOT_FOUND", "User not found: " + userId));

        if (!Objects.equals(idea.getStage(), stageId)) {
            throw new ReviewerBadRequestException("REV_STAGE_MISMATCH", "Discussion allowed only in current stage");
        }

        if (user.getRole() == null || user.getRole() != Constants.Role.REVIEWER) {
        	throw new ReviewerForbiddenException("REV_ONLY_REVIEWER", "Only REVIEWER can post discussion");
        }


        boolean assigned = reviewerAssignRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(ideaId, userId, stageId);
        if (!assigned) throw new ReviewerForbiddenException("REV_NOT_ASSIGNED", "Reviewer not assigned to idea/stage");

        UserActivity parent = resolveAndValidateParent(replyParent, ideaId, stageId);

        int delta = gamificationService.getDeltaForActivity(Constants.ActivityType.REVIEWDISCUSSION);

        UserActivity discussion = UserActivity.builder()
                .idea(idea)
                .user(user)
                .commentText(text)
                .activityType(Constants.ActivityType.REVIEWDISCUSSION)
                .event(EVENT_FOLLOWUP)
                .stageId(stageId)
                .replyParent(parent)
                .delta(delta)
                .createdAt(LocalDateTime.now())
                .deleted(false)
                .build();

        activityRepo.save(discussion);

        if (delta != 0) gamificationService.applyDelta(userId, delta);

        // Notify idea owner about the reviewer discussion
        notificationHelper.notifyReviewerDiscussion(idea, user);
    }

    public List<ReviewerDiscussionDTO> getDiscussionsForStage(Integer ideaId, Integer stageId) {
        return activityRepo.findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalseOrderByCreatedAtAsc(
                        ideaId, Constants.ActivityType.REVIEWDISCUSSION, stageId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public PagedResponse<ReviewerDiscussionDTO> getDiscussionsForStagePaged(Integer ideaId, Integer stageId, Pageable pageable) {
        Page<UserActivity> page = activityRepo.findAllByIdea_IdeaIdAndActivityTypeAndStageIdAndDeletedFalse(
                ideaId, Constants.ActivityType.REVIEWDISCUSSION, stageId, pageable);
        return buildPagedResponse(page.map(this::toDTO));
    }

    private UserActivity resolveAndValidateParent(Integer replyParent, Integer ideaId, Integer stageId) {
        if (replyParent == null) return null;

        UserActivity parent = activityRepo.findById(replyParent)
                .orElseThrow(() -> new ReviewerBadRequestException("REV_PARENT_INVALID", "Invalid replyParent id"));

        if (parent.isDeleted()) throw new ReviewerBadRequestException("REV_PARENT_DELETED", "Parent deleted");
        if (parent.getIdea() == null || !Objects.equals(parent.getIdea().getIdeaId(), ideaId))
            throw new ReviewerBadRequestException("REV_PARENT_OTHER_IDEA", "Parent not in same idea");
        if (!Objects.equals(parent.getStageId(), stageId))
            throw new ReviewerBadRequestException("REV_PARENT_OTHER_STAGE", "Parent not in same stage");
        if (parent.getActivityType() != Constants.ActivityType.REVIEWDISCUSSION)
            throw new ReviewerBadRequestException("REV_PARENT_WRONG_TYPE", "Parent not a review discussion");

        return parent;
    }

    private ReviewerDiscussionDTO toDTO(UserActivity a) {
        return ReviewerDiscussionDTO.builder()
                .userActivityId(a.getUserActivityId())
                .userId(a.getUser().getUserId())
                .displayName(a.getUser().getName())
                .commentText(a.getCommentText())
                .stageId(a.getStageId())
                .replyParent(a.getReplyParent() != null ? a.getReplyParent().getUserActivityId() : null)
                .createdAt(a.getCreatedAt())
                .build();
    }

    private <T> PagedResponse<T> buildPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
