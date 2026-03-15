package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.UserActivity;
import com.ideatrack.main.dto.profilegamification.UserActivityDTO;
import com.ideatrack.main.dto.profilegamification.ApplyDeltaResponseDTO;
import com.ideatrack.main.dto.profilegamification.UserActivityBucketDTO;
import com.ideatrack.main.dto.profilegamification.UserProfileDTO;
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.IIdeaRepository;
import com.ideatrack.main.repository.IProposalRepository;
import com.ideatrack.main.repository.IReportRepository;
import com.ideatrack.main.repository.IUserActivityRepository;
import com.ideatrack.main.repository.IUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GamificationService {

    private final IUserRepository userRepository;
    private final IUserActivityRepository userActivityRepository;
    private final IIdeaRepository ideaRepository;
    private final IProposalRepository proposalRepository;
    private final IReportRepository reportRepository;
    private final UserProfileRules profileRules;

    // Lazy to break the circular dependency: NotificationHelper -> NotificationService -> (no loop)
    // GamificationService -> NotificationHelper would create a cycle without @Lazy.
    @Setter
    @Autowired(required = false)
    @Lazy
    private NotificationHelper notificationHelper;

    private static final String ERR_USER_NOT_FOUND = "User not found";

    private static final Map<Constants.ActivityType, Integer> ACTIVITY_XP_RULES = Map.of(
            Constants.ActivityType.COMMENT, 5,
            Constants.ActivityType.VOTE, 1
    );

    private static final Map<Constants.IdeaStatus, Integer> IDEA_STATUS_XP_RULES = Map.of(
            Constants.IdeaStatus.DRAFT, 0,
            Constants.IdeaStatus.SUBMITTED, 10,
            Constants.IdeaStatus.ACCEPTED, 20,
            Constants.IdeaStatus.REJECTED, 5,
            Constants.IdeaStatus.PROJECTPROPOSAL, 15,
            Constants.IdeaStatus.APPROVED, 25,
            Constants.IdeaStatus.PENDING, -10,
            Constants.IdeaStatus.UNDERREVIEW, 5
    );

    /**
     * Returns the XP delta for a given activity type using configured rules.
     * Null or missing types yield 0.
     */
    public int getDeltaForActivity(Constants.ActivityType type) {
        if (type == null) return 0;
        int delta = ACTIVITY_XP_RULES.getOrDefault(type, 0);
        log.debug("getDeltaForActivity: type={} -> delta={}", type, delta);
        return delta;
    }

    /**
     * Returns the XP delta for a given idea status using configured rules.
     * Null or missing statuses yield 0.
     */
    public int getDeltaForIdeaStatus(Constants.IdeaStatus status) {
        if (status == null) return 0;
        int delta = IDEA_STATUS_XP_RULES.getOrDefault(status, 0);
        log.debug("getDeltaForIdeaStatus: status={} -> delta={}", status, delta);
        return delta;
    }

    /**
     * Applies the delta for the provided idea status to the user's XP.
     * Returns a response containing delta, new total, and reason.
     */
    public ApplyDeltaResponseDTO applyDeltaForIdeaStatus(Integer userId, Constants.IdeaStatus status) {
        log.info("Applying idea-status delta for userId={}, status={}", userId, status);
        int delta = getDeltaForIdeaStatus(status);
        applyDelta(userId, delta);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found while applying idea-status delta, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        ApplyDeltaResponseDTO resp = new ApplyDeltaResponseDTO(
                userId,
                delta,
                user.getTotalXP(),
                "IDEA STATUS",
                status != null ? status.name() : "NULL"
        );
        log.debug("Delta applied for idea status: {}", resp);
        return resp;
    }

    /**
     * Applies the delta for the provided activity type to the user's XP.
     * Returns a response containing delta, new total, and reason.
     */
    public ApplyDeltaResponseDTO applyDeltaForActivityType(Integer userId, Constants.ActivityType type) {
        log.info("Applying activity-type delta for userId={}, type={}", userId, type);
        int delta = getDeltaForActivity(type);
        applyDelta(userId, delta);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found while applying activity-type delta, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        ApplyDeltaResponseDTO resp = new ApplyDeltaResponseDTO(
                userId,
                delta,
                user.getTotalXP(),
                "ACTIVITY",
                type != null ? type.name() : "NULL"
        );
        log.debug("Delta applied for activity type: {}", resp);
        return resp;
    }

    /**
     * Updates the user's total XP by the provided delta and clamps at zero.
     * Persists the updated total XP value.
     */
    public void applyDelta(Integer userId, int delta) {
        log.info("Applying delta for userId={}, delta={}", userId, delta);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found while applying delta, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        String levelBefore = calculateLevel(user.getTotalXP());
        List<String> badgesBefore = calculateBadges(user);

        int newXP = Math.max(0, user.getTotalXP() + delta);
        user.setTotalXP(newXP);
        userRepository.save(user);
        log.debug("Delta applied: userId={}, oldXP={}, delta={}, newXP={}", userId, user.getTotalXP(), delta, newXP);

        // Fire system notifications for newly earned badges or level-up
        if (notificationHelper != null && delta > 0) {
            String levelAfter = calculateLevel(newXP);
            List<String> badgesAfter = calculateBadges(user);

            if (!levelBefore.equals(levelAfter)) {
                notificationHelper.notifyLevelUp(user, levelAfter);
            }

            for (String badge : badgesAfter) {
                if (!badgesBefore.contains(badge)) {
                    notificationHelper.notifyBadgeEarned(user, badge);
                }
            }
        }
    }

    /**
     * Reverses the XP change associated with a specific activity.
     * Returns the reversed delta applied to the user's XP.
     */
    public int undoXPChange(Integer userId, Integer activityId) {
        log.info("Undoing XP change for userId={}, activityId={}", userId, activityId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found while undoing XP change, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        UserActivity original = userActivityRepository.findById(activityId)
                .orElseThrow(() -> {
                    log.warn("Activity not found while undoing XP, activityId={}", activityId);
                    return new ResourceNotFoundException("Activity not found");
                });

        int reversedDelta = -original.getDelta();
        int newXP = Math.max(0, user.getTotalXP() + reversedDelta);
        user.setTotalXP(newXP);
        userRepository.save(user);

        log.debug("XP change undone: userId={}, activityId={}, reversedDelta={}, newXP={}",
                userId, activityId, reversedDelta, newXP);

        return reversedDelta;
    }

    /**
     * Reverts the XP awarded for idea submission by negating the UNDERREVIEW delta.
     * Applies the negative delta to reduce XP accordingly.
     */
    public void undoIdeaSubmissionXP(Integer userId) {
        log.info("Reverting idea submission XP for userId={}", userId);
        int delta = getDeltaForIdeaStatus(Constants.IdeaStatus.UNDERREVIEW);
        applyDelta(userId, -delta);
        log.debug("Reverted UNDERREVIEW delta for userId={}, amount={}", userId, -delta);
    }

    /**
     * Calculates and returns the list of badges earned by the user.
     * Considers role, submission/review counts, XP, votes, comments, and profile completion.
     */
    protected List<String> calculateBadges(User user) {
        log.debug("Calculating badges for userId={}, role={}, totalXP={}",
                user.getUserId(), user.getRole(), user.getTotalXP());

        List<String> badges = new ArrayList<>();
        int xp = user.getTotalXP();

        int ideasSubmitted = ideaRepository.countByUser(user);
        int reviewsDone = ideaRepository.countByUserAndIdeaStatusIn(
                user,
                List.of(Constants.IdeaStatus.ACCEPTED, Constants.IdeaStatus.REJECTED)
        );
        int proposals = proposalRepository.countByUser(user);
        int reports = reportRepository.countByUser(user);
        int commentsMade = userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.COMMENT);
        int votesCast = userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.VOTE);

        log.trace("Badge metrics for userId={}: ideasSubmitted={}, reviewsDone={}, proposals={}, reports={}, commentsMade={}, votesCast={}, xp={}",
                user.getUserId(), ideasSubmitted, reviewsDone, proposals, reports, commentsMade, votesCast, xp);

        Constants.Role role = user.getRole();
        if (role != null) {
            switch (role) {
                case EMPLOYEE -> addEmployeeBadges(badges, ideasSubmitted, xp);
                case REVIEWER -> addReviewerBadges(badges, reviewsDone);
                case ADMIN, SUPERADMIN -> addAdminBadges(badges, proposals, reports);
            }
        }

        addEngagementBadges(badges, commentsMade, votesCast);

        if (profileRules.isProfileCompleted(user)) {
            badges.add("Identity Unlocked");
        }

        log.debug("Badges calculated for userId={}: {}", user.getUserId(), badges);
        return badges;
    }

    private void addEmployeeBadges(List<String> badges, int ideasSubmitted, int xp) {
        if (ideasSubmitted >= 1) badges.add("Spark Igniter");
        if (ideasSubmitted >= 5) badges.add("Idea Adventurer");
        if (ideasSubmitted >= 10) badges.add("Creative Thinker");
        if (ideasSubmitted >= 25) badges.add("Innovation Architect");
        if (ideasSubmitted >= 50) badges.add("Idea Machine");
        if (ideasSubmitted >= 100) badges.add("Visionary Pioneer");

        if (xp >= 500) badges.add("Rising Flame");
        if (xp >= 1000) badges.add("Catalyst of Change");
        if (xp >= 1500) badges.add("Trailblazer");
        if (xp >= 2000) badges.add("Future Shaper");
        if (xp >= 5000) badges.add("Legendary Innovator");
    }

    private void addReviewerBadges(List<String> badges, int reviewsDone) {
        if (reviewsDone >= 1) badges.add("Gatekeeper’s First Verdict");
        if (reviewsDone >= 10) badges.add("Steady Judge");
        if (reviewsDone >= 50) badges.add("Trusted Arbiter");
        if (reviewsDone >= 100) badges.add("Lightning Reviewer");
        if (reviewsDone >= 200) badges.add("Master of Insight");

        badges.add("Eagle Eye");
        badges.add("Quality Sentinel");
    }

    private void addAdminBadges(List<String> badges, int proposals, int reports) {
        if (proposals >= 1) badges.add("Oversight Initiator");
        if (proposals >= 10) badges.add("Proposal Commander");
        if (proposals >= 50) badges.add("Strategic Visionary");

        if (reports >= 1) badges.add("Report Initiator");
        if (reports >= 5) badges.add("Problem Solver");
        if (reports >= 20) badges.add("Wisdom Keeper");
        if (reports >= 50) badges.add("System Architect");

        badges.add("Grand Strategist");
        badges.add("Guardian of Innovation");
    }

    private void addEngagementBadges(List<String> badges, int commentsMade, int votesCast) {
        if (commentsMade >= 1) badges.add("Voice Breaker");
        if (commentsMade >= 10) badges.add("Conversation Starter");
        if (commentsMade >= 50) badges.add("Debate Champion");

        if (votesCast >= 1) badges.add("First Choice");
        if (votesCast >= 10) badges.add("Active Decision Maker");
        if (votesCast >= 100) badges.add("Democracy Driver");
    }

    /**
     * Retrieves XP-changing activities for the user in newest-first order.
     * Returns the list mapped to DTOs.
     */
    public List<UserActivityDTO> getXPChangingActivities(Integer userId) {
        log.info("Fetching XP-changing activities for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found in getXPChangingActivities, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        List<UserActivityDTO> list = userActivityRepository.findByUserAndDeltaNotOrderByCreatedAtDesc(user, 0)
                .stream()
                .map(ua -> toDTO(ua, user))
                .toList();

        log.debug("Found {} XP-changing activities for userId={}", list.size(), userId);
        return list;
    }

    /**
     * Retrieves all interactions for the user in newest-first order.
     * Returns the list mapped to DTOs.
     */
    public List<UserActivityDTO> listInteractions(Integer userId) {
        log.info("Listing interactions for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found in listInteractions, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        List<UserActivityDTO> list = userActivityRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(ua -> toDTO(ua, user))
                .toList();

        log.debug("Found {} interactions for userId={}", list.size(), userId);
        return list;
    }

    /**
     * Retrieves activities filtered by type and returns count with data.
     * Validates inputs and maps to DTOs.
     */
    public UserActivityBucketDTO getActivitiesByType(Integer userId, Constants.ActivityType type) {
        log.info("Getting activities by type for userId={}, type={}", userId, type);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found in getActivitiesByType, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        if (type == null) {
            log.warn("ActivityType is null for userId={}", userId);
            throw new ResourceNotFoundException("ActivityType cannot be null");
        }

        List<UserActivity> filtered = userActivityRepository
                .findByUserAndActivityTypeOrderByCreatedAtDesc(user, type);

        List<UserActivityDTO> data = filtered.stream()
                .map(ua -> toDTO(ua, user))
                .toList();

        UserActivityBucketDTO bucket = new UserActivityBucketDTO(data.size(), data);
        log.debug("getActivitiesByType: userId={}, type={}, count={}", userId, type, data.size());
        return bucket;
    }

    /**
     * Retrieves a pageable slice of XP-changing activities for the user.
     * Returns the slice mapped to DTOs.
     */
    public Slice<UserActivityDTO> getXPChangingActivitiesSlice(Integer userId, Pageable pageable) {
        log.info("Fetching XP-changing activities slice for userId={}, pageable={}", userId, pageable);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found in getXPChangingActivitiesSlice, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        Slice<UserActivity> slice = userActivityRepository
                .findByUserAndDeltaNotOrderByCreatedAtDesc(user, 0, pageable);

        log.debug("Slice fetched (XP-changing): userId={}, contentSize={}, hasNext={}",
                userId, slice.getNumberOfElements(), slice.hasNext());

        return slice.map(ua -> toDTO(ua, user));
    }

    /**
     * Retrieves a pageable slice of all interactions for the user.
     * Returns the slice mapped to DTOs.
     */
    public Slice<UserActivityDTO> listInteractionsSlice(Integer userId, Pageable pageable) {
        log.info("Fetching interactions slice for userId={}, pageable={}", userId, pageable);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found in listInteractionsSlice, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        Slice<UserActivity> slice = userActivityRepository
                .findByUserOrderByCreatedAtDesc(user, pageable);

        log.debug("Slice fetched (interactions): userId={}, contentSize={}, hasNext={}",
                userId, slice.getNumberOfElements(), slice.hasNext());

        return slice.map(ua -> toDTO(ua, user));
    }

    /**
     * Retrieves a pageable slice of interactions filtered by type.
     * Validates inputs and maps the slice to DTOs.
     */
    public Slice<UserActivityDTO> getActivitiesByTypeSlice(Integer userId, Constants.ActivityType type, Pageable pageable) {
        log.info("Fetching activities by type slice for userId={}, type={}, pageable={}", userId, type, pageable);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found in getActivitiesByTypeSlice, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        if (type == null) {
            log.warn("ActivityType is null in getActivitiesByTypeSlice for userId={}", userId);
            throw new ResourceNotFoundException("ActivityType cannot be null");
        }

        Slice<UserActivity> slice = userActivityRepository
                .findByUserAndActivityTypeOrderByCreatedAtDesc(user, type, pageable);

        log.debug("Slice fetched (by type): userId={}, type={}, contentSize={}, hasNext={}",
                userId, type, slice.getNumberOfElements(), slice.hasNext());

        return slice.map(ua -> toDTO(ua, user));
    }

    /**
     * Maps a UserActivity entity to its DTO form with user context.
     * Includes identifiers, content fields, deltas, and timestamps.
     */
    private UserActivityDTO toDTO(UserActivity ua, User user) {
        UserActivityDTO dto = new UserActivityDTO(
                ua.getUserActivityId(),
                user.getUserId(),
                ua.getIdea() != null ? ua.getIdea().getIdeaId() : null,
                ua.getCommentText(),
                ua.getVoteType() != null ? ua.getVoteType().name() : null,
                ua.isSavedIdea(),
                ua.getDelta(),
                user.getTotalXP(),
                ua.getEvent(),
                ua.getActivityType() != null ? ua.getActivityType().name() : null,
                ua.getCreatedAt(),
                ua.getUpdatedAt()
        );
        log.trace("Mapped UserActivity to DTO: activityId={}, userId={}, ideaId={}, delta={}",
                ua.getUserActivityId(), user.getUserId(),
                ua.getIdea() != null ? ua.getIdea().getIdeaId() : null,
                ua.getDelta());
        return dto;
    }

    private static final NavigableMap<Integer, String> XP_TIERS = createXpTiers();

    private static NavigableMap<Integer, String> createXpTiers() {
        NavigableMap<Integer, String> tiers = new TreeMap<>();
        tiers.put(0, "Bronze");
        tiers.put(100, "Silver");
        tiers.put(200, "Ruby");
        tiers.put(300, "Gold");
        tiers.put(400, "Diamond");
        tiers.put(500, "Sapphire");
        tiers.put(600, "Platinum");
        tiers.put(700, "Emerald");
        tiers.put(800, "Opal");
        tiers.put(900, "Amethyst");
        return Collections.unmodifiableNavigableMap(tiers);
    }

    /**
     * Maps total XP to a named level using tier thresholds.
     * Returns the floor tier name for the given XP.
     */
    public String calculateLevel(int totalXP) {
        int xp = Math.max(0, totalXP);
        var entry = XP_TIERS.floorEntry(xp);
        String level = (entry != null) ? entry.getValue() : "Bronze";
        log.debug("calculateLevel: totalXP={} -> level={}", totalXP, level);
        return level;
    }

    /**
     * Computes XP required to reach the next tier from current total.
     * Returns 0 if already at or beyond the highest tier.
     */
    public Integer computeXpToNextLevel(int totalXP) {
        int xp = Math.max(0, totalXP);
        var next = XP_TIERS.higherEntry(xp);
        Integer toNext = (next == null) ? 0 : next.getKey() - xp;
        log.debug("computeXpToNextLevel: totalXP={} -> xpToNext={}", totalXP, toNext);
        return toNext;
    }

    /**
     * Builds a gamified profile for the user including level and badges.
     * Aggregates identity, contact, totals, and profile completion.
     */
    public UserProfileDTO getGamifiedProfile(Integer userId) {
        log.info("Building gamified profile for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found in getGamifiedProfile, userId={}", userId);
                    return new UserNotFoundException(ERR_USER_NOT_FOUND);
                });

        int totalXp = user.getTotalXP();
        String level = calculateLevel(totalXp);
        Integer xpToNext = computeXpToNextLevel(totalXp);

        UserProfileDTO dto = new UserProfileDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNo(),
                user.getBio(),
                user.getProfileUrl(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getDepartment() != null ? user.getDepartment().getDeptName() : null,
                totalXp,
                level,
                xpToNext,
                calculateBadges(user),
                profileRules.isProfileCompleted(user),
                profileRules.getProfileCompletionPercent(user)
        );
        log.debug("Gamified profile built for userId={}: level={}, xpToNext={}, totalXP={}",
                userId, level, xpToNext, totalXp);
        return dto;
    }
}