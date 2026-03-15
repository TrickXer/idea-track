// src/main/java/com/ideatrack/main/controller/GamificationController.java
package com.ideatrack.main.controller;

import com.ideatrack.main.config.AuthUtils; // <-- your existing util
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.dto.profilegamification.UserActivityDTO;
import com.ideatrack.main.dto.profilegamification.ApplyDeltaResponseDTO;
import com.ideatrack.main.dto.profilegamification.UserActivityBucketDTO;
import com.ideatrack.main.dto.profilegamification.UserProfileDTO;
import com.ideatrack.main.repository.IUserRepository; // <-- inject this
import com.ideatrack.main.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Slice;

import java.util.List;

@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','EMPLOYEE','REVIEWER')")
public class GamificationController {

    private final GamificationService gamificationService;
    private final IUserRepository userRepository;

    /**
     * Returns the authenticated user's gamified profile (XP, level, badges, completion).
     * Derives userId from the security context and delegates to the service.
     */
    @GetMapping("/me/xp")
    public ResponseEntity<UserProfileDTO> getMyXP() {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.getGamifiedProfile(userId));
    }

    /**
     * Applies an XP delta for the authenticated user based on the given ActivityType.
     * Computes the delta via rules, updates XP, and returns a simple confirmation message.
     */
    @PostMapping("/me/xp/apply")
    public ResponseEntity<String> applyMyDelta(@RequestParam Constants.ActivityType type) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        int delta = gamificationService.getDeltaForActivity(type);
        gamificationService.applyDelta(userId, delta);
        return ResponseEntity.ok("Applied delta: " + delta);
    }

    /**
     * Reverts the XP change for the authenticated user associated with a specific activityId.
     * Returns the reversed delta applied to the user's total XP.
     */
    @PostMapping("/me/xp/undo/{activityId}")
    public ResponseEntity<String> undoMyXPChange(@PathVariable Integer activityId) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        int reversedDelta = gamificationService.undoXPChange(userId, activityId);
        return ResponseEntity.ok("Reversed delta: " + reversedDelta);
    }

    /**
     * Retrieves only XP-changing activities (delta != 0) for the authenticated user.
     * Results are returned newest-first.
     */
    @GetMapping("/me/xp/history")
    public ResponseEntity<List<UserActivityDTO>> getMyXPChangingActivities() {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.getXPChangingActivities(userId));
    }

    /**
     * Lists all interactions (comments, votes, saves) performed by the authenticated user.
     * Results are returned in chronological order (newest-first).
     */
    @GetMapping("/me/interactions")
    public ResponseEntity<List<UserActivityDTO>> listMyInteractions() {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.listInteractions(userId));
    }

    /**
     * Retrieves interactions for the authenticated user filtered by ActivityType.
     * Returns a compact bucket: { count, data }.
     */
    @GetMapping("/me/interactions/by-type")
    public ResponseEntity<UserActivityBucketDTO> listMyInteractionsByType(
            @RequestParam Constants.ActivityType type) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.getActivitiesByType(userId, type));
    }

    /**
     * Returns a Slice of XP-changing activities for the authenticated user.
     * Supports infinite scrolling with newest-first ordering and hasNext flag.
     */
    @GetMapping("/me/xp/history/page")
    public ResponseEntity<Slice<UserActivityDTO>> getMyXPHistorySlice(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.getXPChangingActivitiesSlice(userId, pageable));
    }

    /**
     * Returns a paginated Slice of all interactions for the authenticated user.
     * Suitable for feeds using “Load more” behavior via hasNext.
     */
    @GetMapping("/me/interactions/page")
    public ResponseEntity<Slice<UserActivityDTO>> listMyInteractionsSlice(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.listInteractionsSlice(userId, pageable));
    }

    /**
     * Retrieves interactions filtered by ActivityType for the authenticated user using Slice pagination.
     * Returns newest-first items and indicates whether more pages exist.
     */
    @GetMapping("/me/interactions/by-type/page")
    public ResponseEntity<Slice<UserActivityDTO>> listMyInteractionsByTypeSlice(
            @RequestParam Constants.ActivityType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.getActivitiesByTypeSlice(userId, type, pageable));
    }

    // ---------- Rules endpoints remain global ----------

    /**
     * Provides the XP delta associated with a specific ActivityType (global, not user-scoped).
     * Useful for UI display and validating scoring rules.
     */
    @GetMapping("/delta/activity")
    public ResponseEntity<Integer> getDeltaForActivity(@RequestParam Constants.ActivityType type) {
        return ResponseEntity.ok(gamificationService.getDeltaForActivity(type));
    }

    /**
     * Provides the XP delta granted when an idea transitions into a specific IdeaStatus (global).
     * Supports XP scoring for idea progression.
     */
    @GetMapping("/delta/idea-status")
    public ResponseEntity<Integer> getDeltaForIdeaStatus(@RequestParam Constants.IdeaStatus status) {
        return ResponseEntity.ok(gamificationService.getDeltaForIdeaStatus(status));
    }

    /**
     * Applies XP for the given ActivityType for the authenticated user using mapped rules.
     * Returns a structured response: { userId, delta, totalXP, type: "ACTIVITY", reason }.
     */
    @PostMapping("/me/xp/apply/activity")
    public ResponseEntity<ApplyDeltaResponseDTO> applyMyDeltaForActivityType(
            @RequestParam Constants.ActivityType type) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.applyDeltaForActivityType(userId, type));
    }

    /**
     * Applies XP for the given IdeaStatus for the authenticated user using mapped rules.
     * Returns a structured response: { userId, delta, totalXP, type: "IDEA STATUS", reason }.
     */
    @PostMapping("/me/xp/apply/idea-status")
    public ResponseEntity<ApplyDeltaResponseDTO> applyMyDeltaForIdeaStatus(
            @RequestParam Constants.IdeaStatus status) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(gamificationService.applyDeltaForIdeaStatus(userId, status));
    }

}