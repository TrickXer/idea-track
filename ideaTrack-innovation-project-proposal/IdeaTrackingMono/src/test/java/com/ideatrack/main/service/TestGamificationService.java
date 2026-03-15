package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Idea;
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestGamificationService {

    @Mock private IUserRepository userRepository;
    @Mock private IUserActivityRepository userActivityRepository;
    @Mock private IIdeaRepository ideaRepository;
    @Mock private IProposalRepository proposalRepository;
    @Mock private IReportRepository reportRepository;
    @Mock private UserProfileRules profileRules;

    @InjectMocks private GamificationService service;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setUserId(1013);
        user.setName("Emp One");
        user.setEmail("emp1@company.com");
        user.setPhoneNo("+91-9000000001");
        user.setTotalXP(120);
        // CHANGED: role is now enum, not String
        user.setRole(Constants.Role.EMPLOYEE);
    }

    // ------------------------------------------------------------
    // Delta helpers
    // ------------------------------------------------------------
    @Test
    @DisplayName("getDeltaForActivity returns configured values with fallback 0")
    void getDeltaForActivity_ok() {
        assertThat(service.getDeltaForActivity(Constants.ActivityType.COMMENT)).isEqualTo(5);
        assertThat(service.getDeltaForActivity(Constants.ActivityType.VOTE)).isEqualTo(1);
        // Not present in map -> fallback 0
        assertThat(service.getDeltaForActivity(Constants.ActivityType.SAVE)).isEqualTo(0);
    }

    @Test
    @DisplayName("getDeltaForIdeaStatus returns configured values with fallback 0")
    void getDeltaForIdeaStatus_ok() {
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.DRAFT)).isEqualTo(0);
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.SUBMITTED)).isEqualTo(10);
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.ACCEPTED)).isEqualTo(20);
        // REJECTED is 5 in the current map
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.REJECTED)).isEqualTo(5);
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.PROJECTPROPOSAL)).isEqualTo(15);
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.APPROVED)).isEqualTo(25);
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.PENDING)).isEqualTo(-10);
        assertThat(service.getDeltaForIdeaStatus(Constants.IdeaStatus.UNDERREVIEW)).isEqualTo(5);
        // Not present / null -> 0
        assertThat(service.getDeltaForIdeaStatus(null)).isEqualTo(0);
    }

    // ------------------------------------------------------------
    // New API: applyDeltaForIdeaStatus / applyDeltaForActivityType
    // ------------------------------------------------------------
    @Test
    @DisplayName("applyDeltaForIdeaStatus applies mapped delta and returns DTO")
    void applyDeltaForIdeaStatus_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        ApplyDeltaResponseDTO dto = service.applyDeltaForIdeaStatus(1013, Constants.IdeaStatus.APPROVED);

        // APPROVED -> +25 per map
        assertThat(dto.getUserId()).isEqualTo(1013);
        assertThat(dto.getDelta()).isEqualTo(25);
        assertThat(dto.getTotalXP()).isEqualTo(145); // 120 + 25
        assertThat(dto.getType()).isEqualTo("IDEA STATUS"); // matches service string
        assertThat(dto.getReason()).isEqualTo("APPROVED");
        verify(userRepository, times(2)).findById(1013); // applyDelta + fetch updated
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("applyDeltaForActivityType applies mapped delta and returns DTO")
    void applyDeltaForActivityType_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        ApplyDeltaResponseDTO dto = service.applyDeltaForActivityType(1013, Constants.ActivityType.COMMENT);

        // COMMENT -> +5 per map
        assertThat(dto.getUserId()).isEqualTo(1013);
        assertThat(dto.getDelta()).isEqualTo(5);
        assertThat(dto.getTotalXP()).isEqualTo(125); // 120 + 5
        assertThat(dto.getType()).isEqualTo("ACTIVITY");
        assertThat(dto.getReason()).isEqualTo("COMMENT");
        verify(userRepository, times(2)).findById(1013); // applyDelta + fetch updated
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("applyDeltaForIdeaStatus -> user missing")
    void applyDeltaForIdeaStatus_userMissing() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.applyDeltaForIdeaStatus(999, Constants.IdeaStatus.SUBMITTED))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("applyDeltaForActivityType -> user missing")
    void applyDeltaForActivityType_userMissing() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.applyDeltaForActivityType(999, Constants.ActivityType.VOTE))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ------------------------------------------------------------
    // applyDelta / undoXPChange / undoIdeaSubmissionXP
    // ------------------------------------------------------------
    @Test
    @DisplayName("applyDelta adds XP and persists clamped to non-negative")
    void applyDelta_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        service.applyDelta(1013, 30);
        assertThat(user.getTotalXP()).isEqualTo(150);
        verify(userRepository).save(user);

        service.applyDelta(1013, -500); // clamp at 0
        assertThat(user.getTotalXP()).isEqualTo(0);
        verify(userRepository, times(2)).save(user);
    }

    @Test
    @DisplayName("applyDelta -> user not found")
    void applyDelta_userMissing() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.applyDelta(999, 10))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("undoXPChange inverts delta and saves user")
    void undoXPChange_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        UserActivity act = mockActivityWithDelta(-10); // original delta -10
        when(userActivityRepository.findById(8001)).thenReturn(Optional.of(act));

        int reversed = service.undoXPChange(1013, 8001);

        assertThat(reversed).isEqualTo(10);
        assertThat(user.getTotalXP()).isEqualTo(130); // 120 + 10
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("undoXPChange -> user missing or activity missing")
    void undoXPChange_missing() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.undoXPChange(999, 8001))
                .isInstanceOf(UserNotFoundException.class);

        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        when(userActivityRepository.findById(9999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.undoXPChange(1013, 9999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("undoIdeaSubmissionXP uses UNDERREVIEW delta (5) and applies negative")
    void undoIdeaSubmissionXP_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        // UNDERREVIEW is 5 in the map; applying -5 should reduce 120 -> 115
        service.undoIdeaSubmissionXP(1013);
        assertThat(user.getTotalXP()).isEqualTo(115);
        verify(userRepository).save(user);
    }

    // ------------------------------------------------------------
    // calculateBadges
    // ------------------------------------------------------------
    @Test
    @DisplayName("calculateBadges for EMPLOYEE role builds threshold + universal badges")
    void calculateBadges_employee() {
        // CHANGED: enum role
        user.setRole(Constants.Role.EMPLOYEE);
        when(ideaRepository.countByUser(user)).thenReturn(12); // triggers 1,5,10 badges
        when(ideaRepository.countByUserAndIdeaStatusIn(eq(user), anyList())).thenReturn(0);
        when(proposalRepository.countByUser(user)).thenReturn(0);
        when(reportRepository.countByUser(user)).thenReturn(0);
        when(userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.COMMENT)).thenReturn(11);
        when(userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.VOTE)).thenReturn(10);
        when(profileRules.isProfileCompleted(user)).thenReturn(true);

        List<String> badges = service.calculateBadges(user);

        assertThat(badges).contains("Spark Igniter", "Idea Adventurer", "Creative Thinker");
        assertThat(badges).contains("Conversation Starter", "Active Decision Maker", "Identity Unlocked");
    }

    @Test
    @DisplayName("calculateBadges for REVIEWER role builds reviewer badges")
    void calculateBadges_reviewer() {
        // CHANGED: enum role
        user.setRole(Constants.Role.REVIEWER);
        when(ideaRepository.countByUser(user)).thenReturn(0);
        when(ideaRepository.countByUserAndIdeaStatusIn(eq(user), anyList())).thenReturn(55);
        when(proposalRepository.countByUser(user)).thenReturn(0);
        when(reportRepository.countByUser(user)).thenReturn(0);
        when(userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.COMMENT)).thenReturn(1);
        when(userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.VOTE)).thenReturn(1);
        when(profileRules.isProfileCompleted(user)).thenReturn(false);

        List<String> badges = service.calculateBadges(user);

        assertThat(badges).contains("Trusted Arbiter", "Eagle Eye", "Quality Sentinel", "First Choice", "Voice Breaker");
    }

    @Test
    @DisplayName("calculateBadges for ADMIN role builds admin badges")
    void calculateBadges_admin() {
        // CHANGED: enum role
        user.setRole(Constants.Role.ADMIN);
        when(ideaRepository.countByUser(user)).thenReturn(0);
        when(ideaRepository.countByUserAndIdeaStatusIn(eq(user), anyList())).thenReturn(0);
        when(proposalRepository.countByUser(user)).thenReturn(12);
        when(reportRepository.countByUser(user)).thenReturn(25);
        when(userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.COMMENT)).thenReturn(0);
        when(userActivityRepository.countByUserAndActivityType(user, Constants.ActivityType.VOTE)).thenReturn(0);
        when(profileRules.isProfileCompleted(user)).thenReturn(false);

        List<String> badges = service.calculateBadges(user);

        assertThat(badges).contains("Proposal Commander", "Wisdom Keeper", "Grand Strategist", "Guardian of Innovation");
    }

    // ------------------------------------------------------------
    // getGamifiedProfile
    // ------------------------------------------------------------
    @Test
    @DisplayName("getGamifiedProfile returns DTO composed from user, tier level, xpToNextLevel, badges, completion")
    void getGamifiedProfile_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        when(profileRules.isProfileCompleted(user)).thenReturn(true);
        // Other counts used inside calculateBadges default to 0 (mock defaults)

        UserProfileDTO dto = service.getGamifiedProfile(1013);

        assertThat(dto.getUserId()).isEqualTo(1013);
        assertThat(dto.getTotalXP()).isEqualTo(120);
        assertThat(dto.getLevel()).isEqualTo("Silver");
        assertThat(dto.getXpToNextLevel()).isEqualTo(80); 
        assertThat(dto.isProfileCompleted()).isTrue();
    }

    @Test
    @DisplayName("getGamifiedProfile -> user missing")
    void getGamifiedProfile_missing() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getGamifiedProfile(999))
                .isInstanceOf(UserNotFoundException.class);
    }

    // ------------------------------------------------------------
    // getXPChangingActivities / listInteractions
    // ------------------------------------------------------------
    @Test
    @DisplayName("getXPChangingActivities maps only delta!=0 newest-first to DTO")
    void getXPChangingActivities_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        var a1 = mockActivityFull(8001, 3001, "Nice!", Constants.VoteType.UPVOTE, true,
                10, "COMMENTED", Constants.ActivityType.COMMENT, LocalDateTime.now().minusMinutes(2));
        var a2 = mockActivityFull(8002, 3002, null, null, false,
                -5, "VOTED", Constants.ActivityType.VOTE, LocalDateTime.now().minusMinutes(1));

        when(userActivityRepository.findByUserAndDeltaNotOrderByCreatedAtDesc(user, 0))
                .thenReturn(List.of(a2, a1)); // newest-first

        List<UserActivityDTO> out = service.getXPChangingActivities(1013);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getId()).isEqualTo(8002);
        assertThat(out.get(1).getId()).isEqualTo(8001);
        assertThat(out.get(0).getActivityType()).isEqualTo("VOTE");
        assertThat(out.get(1).getActivityType()).isEqualTo("COMMENT");
    }

    @Test
    @DisplayName("listInteractions returns all activities newest-first")
    void listInteractions_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        var a1 = mockActivityFull(8101, 3003, "c1", null, false, 0, "COMMENTED",
                Constants.ActivityType.COMMENT, LocalDateTime.now().minusMinutes(3));
        when(userActivityRepository.findByUserOrderByCreatedAtDesc(user))
                .thenReturn(List.of(a1));

        List<UserActivityDTO> out = service.listInteractions(1013);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).getId()).isEqualTo(8101);
        assertThat(out.get(0).getCommentText()).isEqualTo("c1");
    }

    // ------------------------------------------------------------
    // getActivitiesByType (bucket)
    // ------------------------------------------------------------
    @Test
    @DisplayName("getActivitiesByType returns {count, data} for given type")
    void getActivitiesByType_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        var a1 = mockActivityFull(8201, 3011, "text", null, false, 5, "COMMENTED",
                Constants.ActivityType.COMMENT, LocalDateTime.now());
        when(userActivityRepository.findByUserAndActivityTypeOrderByCreatedAtDesc(user, Constants.ActivityType.COMMENT))
                .thenReturn(List.of(a1));

        UserActivityBucketDTO bucket = service.getActivitiesByType(1013, Constants.ActivityType.COMMENT);

        assertThat(bucket.getCount()).isEqualTo(1);
        assertThat(bucket.getData()).hasSize(1);
        assertThat(bucket.getData().get(0).getActivityType()).isEqualTo("COMMENT");
    }

    @Test
    @DisplayName("getActivitiesByType -> user missing or type null")
    void getActivitiesByType_invalid() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getActivitiesByType(999, Constants.ActivityType.VOTE))
                .isInstanceOf(UserNotFoundException.class);

        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.getActivitiesByType(1013, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ActivityType cannot be null");
    }

    // ------------------------------------------------------------
    // Slice variants
    // ------------------------------------------------------------
    @Test
    @DisplayName("getXPChangingActivitiesSlice maps Slice<UserActivity> -> Slice<UserActivityDTO>")
    void getXPChangingActivitiesSlice_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var a = mockActivityFull(8301, 3003, "c", null, false, 10, "COMMENTED",
                Constants.ActivityType.COMMENT, LocalDateTime.now());
        Slice<UserActivity> in = new SliceImpl<>(List.of(a), pageable, true);

        when(userActivityRepository.findByUserAndDeltaNotOrderByCreatedAtDesc(user, 0, pageable))
                .thenReturn(in);

        Slice<UserActivityDTO> out = service.getXPChangingActivitiesSlice(1013, pageable);

        assertThat(out.getContent()).hasSize(1);
        assertThat(out.hasNext()).isTrue();
        assertThat(out.getContent().get(0).getId()).isEqualTo(8301);
    }

    @Test
    @DisplayName("listInteractionsSlice maps Slice<UserActivity> -> Slice<UserActivityDTO>")
    void listInteractionsSlice_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var a = mockActivityFull(8401, 3004, null, null, true, 0, "SAVED",
                Constants.ActivityType.SAVE, LocalDateTime.now());
        Slice<UserActivity> in = new SliceImpl<>(List.of(a), pageable, false);

        when(userActivityRepository.findByUserOrderByCreatedAtDesc(user, pageable))
                .thenReturn(in);

        Slice<UserActivityDTO> out = service.listInteractionsSlice(1013, pageable);

        assertThat(out.getContent()).hasSize(1);
        assertThat(out.hasNext()).isFalse();
        assertThat(out.getContent().get(0).getActivityType()).isEqualTo("SAVE");
    }

    @Test
    @DisplayName("getActivitiesByTypeSlice returns mapped Slice for a given ActivityType")
    void getActivitiesByTypeSlice_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var a = mockActivityFull(8501, 3005, "c", null, false, 0, "COMMENTED",
                Constants.ActivityType.COMMENT, LocalDateTime.now());
        Slice<UserActivity> in = new SliceImpl<>(List.of(a), pageable, true);

        when(userActivityRepository.findByUserAndActivityTypeOrderByCreatedAtDesc(user, Constants.ActivityType.COMMENT, pageable))
                .thenReturn(in);

        Slice<UserActivityDTO> out = service.getActivitiesByTypeSlice(1013, Constants.ActivityType.COMMENT, pageable);

        assertThat(out.getContent()).hasSize(1);
        assertThat(out.hasNext()).isTrue();
        assertThat(out.getContent().get(0).getActivityType()).isEqualTo("COMMENT");
    }

    @Test
    @DisplayName("getActivitiesByTypeSlice -> user missing or type null")
    void getActivitiesByTypeSlice_invalid() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> service.getActivitiesByTypeSlice(999, Constants.ActivityType.VOTE, pageable))
                .isInstanceOf(UserNotFoundException.class);

        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.getActivitiesByTypeSlice(1013, null, pageable))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("ActivityType cannot be null");
    }

    // ------------------------------------------------------------
    // calculateLevel
    // ------------------------------------------------------------
    @Test
    @DisplayName("calculateLevel maps XP to named tiers (inclusive thresholds)")
    void calculateLevel_ok() {
        assertThat(service.calculateLevel(0)).isEqualTo("Bronze");
        assertThat(service.calculateLevel(99)).isEqualTo("Bronze");
        assertThat(service.calculateLevel(100)).isEqualTo("Silver");
        assertThat(service.calculateLevel(250)).isEqualTo("Ruby");
    }

    // ---------- Helpers to build mocked activities used by toDTO ----------
    private UserActivity mockActivityWithDelta(int delta) {
        UserActivity ua = mock(UserActivity.class);
        when(ua.getDelta()).thenReturn(delta);
        return ua;
    }

    private UserActivity mockActivityFull(
            int id,
            Integer ideaId,
            String commentText,
            Constants.VoteType voteType,
            boolean saved,
            int delta,
            String event,
            Constants.ActivityType activityType,
            LocalDateTime createdAt
    ) {
        UserActivity ua = mock(UserActivity.class);
        Idea idea = null;
        if (ideaId != null) {
            idea = mock(Idea.class);
            when(idea.getIdeaId()).thenReturn(ideaId);
        }
        when(ua.getUserActivityId()).thenReturn(id);
        when(ua.getIdea()).thenReturn(idea);
        when(ua.getCommentText()).thenReturn(commentText);
        when(ua.getVoteType()).thenReturn(voteType);
        when(ua.isSavedIdea()).thenReturn(saved);
        when(ua.getDelta()).thenReturn(delta);
        when(ua.getEvent()).thenReturn(event);
        when(ua.getActivityType()).thenReturn(activityType);
        when(ua.getCreatedAt()).thenReturn(createdAt);
        when(ua.getUpdatedAt()).thenReturn(createdAt.plusSeconds(5));
        return ua;
    }
}