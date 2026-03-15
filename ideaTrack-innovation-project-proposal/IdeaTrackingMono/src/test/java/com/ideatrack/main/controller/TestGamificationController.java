package com.ideatrack.main.controller;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.profilegamification.UserActivityDTO;
import com.ideatrack.main.dto.profilegamification.ApplyDeltaResponseDTO;
import com.ideatrack.main.dto.profilegamification.UserActivityBucketDTO;
import com.ideatrack.main.dto.profilegamification.UserProfileDTO;
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.repository.IUserRepository;
import com.ideatrack.main.service.GamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@WithMockUser(
        username = "emp1@company.com",
        authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestGamificationController {

    @Autowired
    private GamificationController controller;

    @MockitoBean
    private GamificationService service;

    @MockitoBean
    private IUserRepository userRepository;

    private static final int AUTH_USER_ID = 1013;
    private static final String AUTH_EMAIL = "emp1@company.com";

    @BeforeEach
    void setupAuthUserResolution() {
        Mockito.when(userRepository.findByEmail(AUTH_EMAIL))
                .thenReturn(Optional.of(mockUser(AUTH_USER_ID, AUTH_EMAIL)));
    }

    // ------------------------------------------------------------
    // 1) GET /api/gamification/me/xp
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/xp - Success")
    void getMyXP_ok() {
        UserProfileDTO dto = new UserProfileDTO(
                AUTH_USER_ID,                     // userId
                "Emp 1",                          // name
                AUTH_EMAIL,                       // email
                "+91-9000000001",                 // phoneNo
                "bio",                            // bio
                "/u/p.png",                       // profileUrl
                "EMPLOYEE",                       // role
                "Engineering",                    // departmentName
                120,                              // totalXP
                "Silver",                         // level
                80,                               // xpToNextLevel
                List.of("Spark"),                 // badges
                true,                             // profileCompleted
                75                                // profileCompletionPercent
        );

        Mockito.when(service.getGamifiedProfile(AUTH_USER_ID)).thenReturn(dto);

        ResponseEntity<UserProfileDTO> resp = controller.getMyXP();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getUserId()).isEqualTo(AUTH_USER_ID);
        assertThat(resp.getBody().getLevel()).isEqualTo("Silver");
        assertThat(resp.getBody().getXpToNextLevel()).isEqualTo(80);
        assertThat(resp.getBody().getRole()).isEqualTo("EMPLOYEE");
        assertThat(resp.getBody().getProfileCompletionPercent()).isEqualTo(75);
    }

    @Test
    @DisplayName("GET /me/xp - throws ResourceNotFoundException from service")
    void getMyXP_userNotFound() {
        Mockito.when(service.getGamifiedProfile(AUTH_USER_ID))
                .thenThrow(new ResourceNotFoundException("User not found"));
        try {
            controller.getMyXP();
            org.junit.jupiter.api.Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException ex) {
            assertThat(ex.getMessage()).isEqualTo("User not found");
        }
    }

    // ------------------------------------------------------------
    // 2) POST /me/xp/apply (String response)
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST /me/xp/apply - Success")
    void applyMyDelta_ok() {
        Mockito.when(service.getDeltaForActivity(Constants.ActivityType.COMMENT)).thenReturn(5);

        ResponseEntity<String> resp = controller.applyMyDelta(Constants.ActivityType.COMMENT);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Applied delta: 5");
        Mockito.verify(service).applyDelta(AUTH_USER_ID, 5);
    }

    // ------------------------------------------------------------
    // 3) POST /me/xp/undo/{activityId} (String response)
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST /me/xp/undo/{activityId} - Success")
    void undoMyXPChange_ok() {
        Mockito.when(service.undoXPChange(AUTH_USER_ID, 8001)).thenReturn(-10);

        ResponseEntity<String> resp = controller.undoMyXPChange(8001);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Reversed delta: -10");
    }

    // ------------------------------------------------------------
    // 4) GET /me/xp/history
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/xp/history - Success")
    void getMyXPChangingActivities_ok() {
        UserActivityDTO a = sampleActivity(8001, AUTH_USER_ID, 3001, "COMMENT", 10);
        Mockito.when(service.getXPChangingActivities(AUTH_USER_ID)).thenReturn(List.of(a));

        ResponseEntity<List<UserActivityDTO>> resp = controller.getMyXPChangingActivities();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getId()).isEqualTo(8001);
    }

    // ------------------------------------------------------------
    // 5) GET /me/interactions
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/interactions - Success")
    void listMyInteractions_ok() {
        UserActivityDTO a = sampleActivity(8101, AUTH_USER_ID, 3002, "VOTE", 1);
        Mockito.when(service.listInteractions(AUTH_USER_ID)).thenReturn(List.of(a));

        ResponseEntity<List<UserActivityDTO>> resp = controller.listMyInteractions();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getActivityType()).isEqualTo("VOTE");
    }

    // ------------------------------------------------------------
    // 6) GET /me/interactions/by-type
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/interactions/by-type - Success")
    void listMyInteractionsByType_ok() {
        UserActivityDTO a = sampleActivity(8201, AUTH_USER_ID, 3011, "COMMENT", 5);
        UserActivityBucketDTO bucket = new UserActivityBucketDTO(1, List.of(a));

        Mockito.when(service.getActivitiesByType(AUTH_USER_ID, Constants.ActivityType.COMMENT))
                .thenReturn(bucket);

        ResponseEntity<UserActivityBucketDTO> resp =
                controller.listMyInteractionsByType(Constants.ActivityType.COMMENT);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getCount()).isEqualTo(1L); // or 1 depending on your DTO type
        assertThat(resp.getBody().getData()).hasSize(1);
        assertThat(resp.getBody().getData().get(0).getActivityType()).isEqualTo("COMMENT");
    }

    // ------------------------------------------------------------
    // 7) GET /me/xp/history/page -> Slice
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/xp/history/page - Slice Success")
    void getMyXPHistorySlice_ok() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserActivityDTO> content = List.of(sampleActivity(8301, AUTH_USER_ID, 3003, "COMMENT", 10));
        Slice<UserActivityDTO> slice = new SliceImpl<>(content, pageable, true);

        Mockito.when(service.getXPChangingActivitiesSlice(AUTH_USER_ID, pageable)).thenReturn(slice);

        ResponseEntity<Slice<UserActivityDTO>> resp = controller.getMyXPHistorySlice(pageable);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getContent()).hasSize(1);
        assertThat(resp.getBody().hasNext()).isTrue();
    }

    // ------------------------------------------------------------
    // 8) GET /me/interactions/page -> Slice
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/interactions/page - Slice Success")
    void listMyInteractionsSlice_ok() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<UserActivityDTO> slice = new SliceImpl<>(
                List.of(sampleActivity(8401, AUTH_USER_ID, 3004, "SAVE", 2)), pageable, false
        );

        Mockito.when(service.listInteractionsSlice(AUTH_USER_ID, pageable)).thenReturn(slice);

        ResponseEntity<Slice<UserActivityDTO>> resp = controller.listMyInteractionsSlice(pageable);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getContent()).hasSize(1);
        assertThat(resp.getBody().hasNext()).isFalse();
    }

    // ------------------------------------------------------------
    // 9) GET /me/interactions/by-type/page -> Slice
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/interactions/by-type/page - Slice Success")
    void listMyInteractionsByTypeSlice_ok() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Slice<UserActivityDTO> slice = new SliceImpl<>(
                List.of(sampleActivity(8501, AUTH_USER_ID, 3005, "COMMENT", 3)), pageable, true
        );

        Mockito.when(service.getActivitiesByTypeSlice(AUTH_USER_ID, Constants.ActivityType.COMMENT, pageable))
                .thenReturn(slice);

        ResponseEntity<Slice<UserActivityDTO>> resp =
                controller.listMyInteractionsByTypeSlice(Constants.ActivityType.COMMENT, pageable);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getContent()).hasSize(1);
        assertThat(resp.getBody().hasNext()).isTrue();
    }

    // ------------------------------------------------------------
    // 10) GET /delta endpoints (unchanged)
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /delta/activity - Success")
    void getDeltaForActivity_ok() {
        Mockito.when(service.getDeltaForActivity(Constants.ActivityType.VOTE)).thenReturn(10);
        ResponseEntity<Integer> resp = controller.getDeltaForActivity(Constants.ActivityType.VOTE);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(10);
    }

    @Test
    @DisplayName("GET /delta/idea-status - Success")
    void getDeltaForIdeaStatus_ok() {
        Mockito.when(service.getDeltaForIdeaStatus(Constants.IdeaStatus.APPROVED)).thenReturn(25);
        ResponseEntity<Integer> resp = controller.getDeltaForIdeaStatus(Constants.IdeaStatus.APPROVED);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo(25);
    }

    // ------------------------------------------------------------
    // 11) POST /me/xp/apply/activity -> ApplyDeltaResponseDTO
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST /me/xp/apply/activity - returns ApplyDeltaResponseDTO")
    void applyMyDeltaForActivityType_ok() {
        ApplyDeltaResponseDTO dto = new ApplyDeltaResponseDTO(AUTH_USER_ID, 5, 125, "ACTIVITY", "COMMENT");
        Mockito.when(service.applyDeltaForActivityType(AUTH_USER_ID, Constants.ActivityType.COMMENT)).thenReturn(dto);

        ResponseEntity<ApplyDeltaResponseDTO> resp =
                controller.applyMyDeltaForActivityType(Constants.ActivityType.COMMENT);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getUserId()).isEqualTo(AUTH_USER_ID);
        assertThat(resp.getBody().getDelta()).isEqualTo(5);
        assertThat(resp.getBody().getTotalXP()).isEqualTo(125);
        assertThat(resp.getBody().getType()).isEqualTo("ACTIVITY");
        assertThat(resp.getBody().getReason()).isEqualTo("COMMENT");
        Mockito.verify(service).applyDeltaForActivityType(AUTH_USER_ID, Constants.ActivityType.COMMENT);
    }

    // ------------------------------------------------------------
    // 12) POST /me/xp/apply/idea-status -> ApplyDeltaResponseDTO
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST /me/xp/apply/idea-status - returns ApplyDeltaResponseDTO")
    void applyMyDeltaForIdeaStatus_ok() {
        ApplyDeltaResponseDTO dto = new ApplyDeltaResponseDTO(AUTH_USER_ID, 25, 145, "IDEA STATUS", "APPROVED");
        Mockito.when(service.applyDeltaForIdeaStatus(AUTH_USER_ID, Constants.IdeaStatus.APPROVED)).thenReturn(dto);

        ResponseEntity<ApplyDeltaResponseDTO> resp =
                controller.applyMyDeltaForIdeaStatus(Constants.IdeaStatus.APPROVED);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().getUserId()).isEqualTo(AUTH_USER_ID);
        assertThat(resp.getBody().getDelta()).isEqualTo(25);
        assertThat(resp.getBody().getTotalXP()).isEqualTo(145);
        assertThat(resp.getBody().getType()).isEqualTo("IDEA STATUS");
        assertThat(resp.getBody().getReason()).isEqualTo("APPROVED");
        Mockito.verify(service).applyDeltaForIdeaStatus(AUTH_USER_ID, Constants.IdeaStatus.APPROVED);
    }

    // ------------------------------------------------------------
    // Negative examples (service throws) using /me
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /me/interactions/by-type - throws ResourceNotFoundException from service")
    void listMyInteractionsByType_userMissing() {
        Mockito.when(service.getActivitiesByType(eq(AUTH_USER_ID), any(Constants.ActivityType.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        try {
            controller.listMyInteractionsByType(Constants.ActivityType.COMMENT);
            org.junit.jupiter.api.Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException ex) {
            assertThat(ex.getMessage()).isEqualTo("User not found");
        }
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private User mockUser(int id, String email) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        return u;
    }

    private UserActivityDTO sampleActivity(int id, int userId, int ideaId, String type, int delta) {
        return new UserActivityDTO(
                id,
                userId,
                ideaId,
                type.equals("COMMENT") ? "text" : null,
                type.equals("VOTE") ? "UPVOTE" : null,
                type.equals("SAVE"),
                delta,
                120 + delta,
                "reason",
                type,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now()
        );
    }
}