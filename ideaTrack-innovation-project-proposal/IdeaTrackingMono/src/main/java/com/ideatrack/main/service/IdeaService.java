package com.ideatrack.main.service;

import com.ideatrack.main.data.*;
import com.ideatrack.main.dto.*;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IdeaService {

    private final IIdeaRepository ideaRepository;
    private final IUserRepository userRepository;
    private final ICategoryRepository categoryRepository;
    private final IUserActivityRepository userActivityRepository;
    private final IAssignedReviewerToIdeaRepository reviewerRepository;
    private final GamificationService gamificationService;
    private final NotificationHelper notificationHelper;

    // -------------------------
    // 1. Lifecycle Operations
    // -------------------------

    public IdeaResponse createIdea(IdeaCreateRequest req, String authEmail) {
        log.info("createIdea: title='{}', user='{}'", req.getTitle(), authEmail);
        
        User user = userRepository.findByEmail(authEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + authEmail));
        
        Category category = requireCategory(req.getCategoryId());

        Idea idea = new Idea();
        idea.setTitle(req.getTitle());
        idea.setDescription(req.getDescription());
        idea.setProblemStatement(req.getProblemStatement());
        idea.setUser(user);
        idea.setCategory(category);
        idea.setTag(req.getTag());
        idea.setThumbnailURL(req.getThumbnailURL());
        idea.setStage(0);
        idea.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        idea.setDeleted(false);

        return toResponse(ideaRepository.save(idea), user.getUserId(), false);
    }

    @PreAuthorize("@ideaService.isOwner(#ideaId, authentication.name)")
    public IdeaResponse updateIdea(Integer ideaId, IdeaUpdateRequest req) {
        Idea idea = requireIdea(ideaId);

        if (idea.getIdeaStatus() != Constants.IdeaStatus.DRAFT && 
            idea.getIdeaStatus() != Constants.IdeaStatus.REFINE) {
            throw new IllegalStateException("Only Draft or Refine ideas can be edited.");
        }

        if (req.getTitle() != null) idea.setTitle(req.getTitle());
        if (req.getDescription() != null) idea.setDescription(req.getDescription());
        if (req.getProblemStatement() != null) idea.setProblemStatement(req.getProblemStatement());
        if (req.getTag() != null) idea.setTag(req.getTag());
        if (req.getThumbnailURL() != null) idea.setThumbnailURL(req.getThumbnailURL());
        
        if (req.getCategoryId() != null) {
            idea.setCategory(requireCategory(req.getCategoryId()));
        }

        Idea savedIdea = ideaRepository.save(idea);
        return toResponse(savedIdea, savedIdea.getUser().getUserId(), false);
    }
    
    @PreAuthorize("@ideaService.isOwner(#ideaId, authentication.name)")
    public IdeaResponse submitIdea(Integer ideaId) {
        Idea idea = requireIdea(ideaId);
        
        if (idea.getIdeaStatus() != Constants.IdeaStatus.DRAFT && 
            idea.getIdeaStatus() != Constants.IdeaStatus.REFINE) {
            throw new IllegalStateException("Only Draft ideas can be submitted.");
        }

        // When resubmitting from REFINE, clear stale reviewer decisions so
        // reviewers can vote again on the updated idea.
        if (idea.getIdeaStatus() == Constants.IdeaStatus.REFINE && idea.getStage() != null) {
            reviewerRepository.resetDecisionsForStage(ideaId, idea.getStage());
        }

        Integer authorId = idea.getUser().getUserId();
        User author = idea.getUser();
        idea.setIdeaStatus(Constants.IdeaStatus.SUBMITTED);

        int xpDelta = gamificationService.getDeltaForIdeaStatus(Constants.IdeaStatus.SUBMITTED);
        gamificationService.applyDelta(authorId, xpDelta);

        Idea savedIdea = ideaRepository.save(idea);
        notificationHelper.notifyIdeaSubmitted(savedIdea, author);
        return toResponse(savedIdea, authorId, false);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN') or @ideaService.isOwner(#ideaId, authentication.name)")
    public void softDeleteIdea(Integer ideaId) {
        log.info("softDeleteIdea: ideaId={}", ideaId);
        Idea idea = requireIdea(ideaId);
        
        // If it was already submitted, we might need to rollback XP
        if (idea.getIdeaStatus() != Constants.IdeaStatus.DRAFT && !idea.isDeleted()) {
            gamificationService.undoIdeaSubmissionXP(idea.getUser().getUserId());
        }
        
        idea.setDeleted(true);
        ideaRepository.save(idea);
    }

    // -------------------------
    // 2. Query Operations
    // -------------------------
    
    @Transactional(readOnly = true)
    public List<IdeaResponse> getIdeasByUser(Integer userId) {
        List<Idea> ideas = ideaRepository.findAllByUser_UserIdAndDeletedFalse(userId);
        return ideas.stream()
                .map(idea -> toResponse(idea, userId, true)) 
                .toList();
    }
    
    @Transactional(readOnly = true)
    public IdeaResponse getIdea(Integer ideaId, Integer viewerUserId) {
        return toResponse(requireIdea(ideaId), viewerUserId, false);
    }

    @Transactional(readOnly = true)
    public PagedResponse<IdeaResponse> searchIdeas(
            String q, Integer categoryId, Integer userId, Constants.IdeaStatus status,
            Integer stage, Boolean includeDeleted, Pageable pageable, Integer viewerUserId) {

        boolean incDel = includeDeleted != null && includeDeleted;
        Page<Idea> page = ideaRepository.searchIdeas(
                emptyToNull(q), categoryId, userId, status, stage, incDel, pageable);

        return buildPagedResponse(page.map(i -> toResponse(i, viewerUserId, true)));
    }

    // -------------------------
    // Helpers & Mappers 
    // -------------------------
    
    private Idea requireIdea(Integer id) {
        return ideaRepository.findById(id)
                .orElseThrow(() -> new IdeaNotFound("Idea not found: " + id));
    }

    private Category requireCategory(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFound("Category not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Integer ideaId, String email) {
        return ideaRepository.findById(ideaId)
                .map(idea -> idea.getUser().getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }
    
    private IdeaResponse toResponse(Idea idea, Integer viewerUserId, boolean forList) {
        long up = countActivity(idea.getIdeaId(), Constants.ActivityType.VOTE, Constants.VoteType.UPVOTE);
        long down = countActivity(idea.getIdeaId(), Constants.ActivityType.VOTE, Constants.VoteType.DOWNVOTE);
        long comments = countActivity(idea.getIdeaId(), Constants.ActivityType.COMMENT, null);
        
        List<String> feedback = reviewerRepository
                .findAllByIdea_IdeaIdAndFeedbackIsNotNullOrderByUpdatedAtDesc(idea.getIdeaId())
                .stream()
                .map(AssignedReviewerToIdea::getFeedback)
                .toList();

        return IdeaResponse.builder()
                .ideaId(idea.getIdeaId())
                .title(idea.getTitle())
                .description(forList ? null : idea.getDescription())
                .problemStatement(forList ? null : idea.getProblemStatement())
                .tag(idea.getTag())
                .thumbnailURL(idea.getThumbnailURL())
                .category(mapCategory(idea.getCategory()))
                .author(mapAuthor(idea.getUser()))
                .ideaStatus(idea.getIdeaStatus())
                .stage(idea.getStage())
                .feedback(feedback) 
                .votes(VoteCountsDTO.builder().upvotes(up).downvotes(down).build())
                .commentsCount(comments)
                .viewer(mapViewerStatus(idea, viewerUserId))
                .createdAt(idea.getCreatedAt())
                .build();
    }
    
    private ViewerStatusDTO mapViewerStatus(Idea idea, Integer viewerUserId) {
        if (viewerUserId == null) return null;
        
        boolean isOwner = idea.getUser() != null && idea.getUser().getUserId().equals(viewerUserId);

        // FIX: Use a method that takes BOTH ideaId and userId
        Optional<UserActivity> vote = userActivityRepository.findFirstByIdea_IdeaIdAndUser_UserIdAndActivityTypeAndDeletedFalse(
                idea.getIdeaId(), viewerUserId, Constants.ActivityType.VOTE);
                
        Optional<UserActivity> saved = userActivityRepository.findFirstByIdea_IdeaIdAndUser_UserIdAndActivityTypeAndDeletedFalse(
                idea.getIdeaId(), viewerUserId, Constants.ActivityType.SAVE);

        return ViewerStatusDTO.builder()
                .owner(isOwner)
                .voteType(vote.map(UserActivity::getVoteType).orElse(null))
                .saved(saved.isPresent())
                .build();
    }

    private CategoryLiteDTO mapCategory(Category cat) {
        if (cat == null) return null;
        return CategoryLiteDTO.builder().categoryId(cat.getCategoryId()).name(cat.getName()).build();
    }

    private UserLiteDTO mapAuthor(User user) {
        if (user == null) return null;
        return UserLiteDTO.builder().userId(user.getUserId()).displayName(user.getName()).build();
    }

    private long countActivity(Integer ideaId, Constants.ActivityType type, Constants.VoteType voteType) {
        if (voteType != null) {
            return userActivityRepository.countByIdea_IdeaIdAndActivityTypeAndVoteTypeAndDeletedFalse(ideaId, type, voteType);
        }
        return userActivityRepository.countByIdea_IdeaIdAndActivityTypeAndDeletedFalse(ideaId, type);
    }

    private <T> PagedResponse<T> buildPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent()).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .first(page.isFirst()).last(page.isLast()).build();
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}