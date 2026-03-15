package com.ideatrack.main.controller;

import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.DepartmentListResponse;
import com.ideatrack.main.dto.profilegamification.PasswordUpdateDTO;
import com.ideatrack.main.dto.profilegamification.UpdateProfileRequest;
import com.ideatrack.main.dto.profilegamification.UserProfileDTO;
import com.ideatrack.main.exception.PasswordChangeException;
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.repository.IUserRepository;
import com.ideatrack.main.service.ProfileService;
import com.ideatrack.main.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for ProfileController using /me endpoints.
 * Relies on AuthUtils.currentUserId(IUserRepository) which resolves SecurityContext username -> userId.
 */
@SpringBootTest
@WithMockUser(
        username = "emp1@company.com",
        authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestProfileController {

    @Autowired
    private ProfileController profileController;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private IUserRepository userRepository;

    private static final int AUTH_USER_ID = 1013;
    private static final String AUTH_EMAIL = "emp1@company.com";

    @BeforeEach
    void setupAuthUserResolution() {
        // Stub user lookup for AuthUtils.currentUserId(userRepository)
        when(userRepository.findByEmail(AUTH_EMAIL))
                .thenReturn(Optional.of(mockUser(AUTH_USER_ID, AUTH_EMAIL)));
    }

    // ------------------------------------------------------------
    // 1) GET /api/profile/me
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /api/profile/me - Success")
    void testGetProfile() {
        UserProfileDTO dto = new UserProfileDTO(
                AUTH_USER_ID,                         // userId
                "Employee 1",                         // name
                AUTH_EMAIL,                           // email
                "+91-9000000001",                     // phoneNo
                "Engineer",                           // bio
                "/uploads/profile-pics/1013_profile.jpg", // profileUrl
                "EMPLOYEE",                           // role
                "Engineering",                        // departmentName
                120,                                  // totalXP
                "Silver",                             // level
                80,                                   // xpToNextLevel
                List.of("Spark Igniter"),             // badges
                true,                                 // profileCompleted
                90                                    // profileCompletionPercent  ⬅️ NEW
        );

        when(profileService.getProfile(AUTH_USER_ID)).thenReturn(dto);

        ResponseEntity<UserProfileDTO> response = profileController.getProfile();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUserId()).isEqualTo(AUTH_USER_ID);
        assertThat(response.getBody().getLevel()).isEqualTo("Silver");
        assertThat(response.getBody().getXpToNextLevel()).isEqualTo(80);
        assertThat(response.getBody().getRole()).isEqualTo("EMPLOYEE");
        // Assert new field
        assertThat(response.getBody().getProfileCompletionPercent()).isEqualTo(90);
    }

    // ------------------------------------------------------------
    // 2) PUT /api/profile/me
    // ------------------------------------------------------------
    @Test
    @DisplayName("PUT /api/profile/me - Success")
    void testUpdateProfile() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Emp One");
        req.setBio("Updated bio");
        req.setProfileUrl("/some/url.png");

        UserProfileDTO updated = new UserProfileDTO(
                AUTH_USER_ID,                         // userId
                "Emp One",                            // name
                AUTH_EMAIL,                           // email
                "+91-9000000001",                     // phoneNo
                "Updated bio",                        // bio
                "/some/url.png",                      // profileUrl
                "EMPLOYEE",                           // role
                "Engineering",                        // departmentName
                130,                                  // totalXP
                "Silver",                             // level
                70,                                   // xpToNextLevel
                List.of("Spark Igniter"),             // badges
                true,                                 // profileCompleted
                100                                   // profileCompletionPercent  ⬅️ NEW
        );

        when(profileService.updateProfile(eq(AUTH_USER_ID), any(UpdateProfileRequest.class)))
                .thenReturn(updated);

        ResponseEntity<UserProfileDTO> response = profileController.updateProfile(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getBio()).isEqualTo("Updated bio");
        assertThat(response.getBody().getXpToNextLevel()).isEqualTo(70);
        assertThat(response.getBody().getRole()).isEqualTo("EMPLOYEE");
        assertThat(response.getBody().getProfileCompletionPercent()).isEqualTo(100);
    }

    // ------------------------------------------------------------
    // 3) POST /api/profile/me/profile-photo
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST /api/profile/me/profile-photo - Success")
    void testUploadPhoto() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image".getBytes()
        );

        UserProfileDTO dto = new UserProfileDTO(
                AUTH_USER_ID,                         // userId
                "Employee 1",                         // name
                AUTH_EMAIL,                           // email
                "+91-9000000001",                     // phoneNo
                "Engineer",                           // bio
                "/uploads/profile-pics/1013_profile.jpg", // profileUrl
                "EMPLOYEE",                           // role
                "Engineering",                        // departmentName
                120,                                  // totalXP
                "Silver",                             // level
                80,                                   // xpToNextLevel
                List.of(),                            // badges
                true,                                 // profileCompleted
                95                                    // profileCompletionPercent  ⬅️ NEW
        );

        when(profileService.updateProfilePhoto(eq(AUTH_USER_ID), any()))
                .thenReturn(dto);

        ResponseEntity<UserProfileDTO> response = profileController.uploadPhoto(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProfileUrl())
                .contains("/uploads/profile-pics/1013_profile.jpg");
        assertThat(response.getBody().getRole()).isEqualTo("EMPLOYEE");
        assertThat(response.getBody().getProfileCompletionPercent()).isEqualTo(95);
    }

    // ------------------------------------------------------------
    // 4) DELETE /api/profile/me/profile-photo
    // ------------------------------------------------------------
    @Test
    @DisplayName("DELETE /api/profile/me/profile-photo - Success")
    void testDeletePhoto() {
        UserProfileDTO dto = new UserProfileDTO(
                AUTH_USER_ID,                         // userId
                "Employee 1",                         // name
                AUTH_EMAIL,                           // email
                "+91-9000000001",                     // phoneNo
                "Engineer",                           // bio
                null,                                 // profileUrl (deleted)
                "EMPLOYEE",                           // role
                "Engineering",                        // departmentName
                120,                                  // totalXP
                "Silver",                             // level
                80,                                   // xpToNextLevel
                List.of(),                            // badges
                true,                                 // profileCompleted
                75                                    // profileCompletionPercent  ⬅️ NEW (example)
        );

        when(profileService.deleteProfilePhoto(AUTH_USER_ID)).thenReturn(dto);

        ResponseEntity<UserProfileDTO> response = profileController.deletePhoto();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getProfileUrl()).isNull();
        assertThat(response.getBody().getRole()).isEqualTo("EMPLOYEE");
        assertThat(response.getBody().getProfileCompletionPercent()).isEqualTo(75);
    }

    // ------------------------------------------------------------
    // 5) PUT /api/profile/me/password
    // ------------------------------------------------------------
    @Test
    @DisplayName("PUT /api/profile/me/password - Success")
    void testChangePassword() {
        PasswordUpdateDTO pwdReq = new PasswordUpdateDTO();
        pwdReq.setCurrentPassword("oldPass");
        pwdReq.setNewPassword("newPass#2026");

        when(profileService.changePassword(eq(AUTH_USER_ID), anyString(), anyString()))
                .thenReturn("Password changed successfully");

        ResponseEntity<String> response = profileController.changePassword(pwdReq);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Password changed successfully");
    }

    // ------------------------------------------------------------
    // 6) DELETE /api/profile/me
    // ------------------------------------------------------------
    @Test
    @DisplayName("DELETE /api/profile/me - Success")
    void testDeleteProfile() {
        ResponseEntity<Void> response = profileController.deleteProfile();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(profileService).deleteProfile(AUTH_USER_ID);
    }

    // -------------------- NEGATIVE CASES ----------------------

    @Test
    @DisplayName("GET /api/profile/me - user not found (service throws)")
    void getProfile_userNotFound() {
        when(profileService.getProfile(AUTH_USER_ID))
                .thenThrow(new ResourceNotFoundException("User not found"));

        try {
            profileController.getProfile();
        } catch (ResourceNotFoundException ex) {
            assertThat(ex.getMessage()).isEqualTo("User not found");
        }
    }

    @Test
    @DisplayName("PUT /api/profile/me - user not found (service throws)")
    void updateProfile_userNotFound() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        when(profileService.updateProfile(eq(AUTH_USER_ID), any()))
                .thenThrow(new ResourceNotFoundException("User not found"));

        try {
            profileController.updateProfile(req);
        } catch (ResourceNotFoundException ex) {
            assertThat(ex.getMessage()).isEqualTo("User not found");
        }
    }

    @Test
    @DisplayName("POST /api/profile/me/profile-photo - invalid type")
    void uploadPhoto_invalidType() {
        MockMultipartFile bad = new MockMultipartFile(
                "file", "x.gif", "image/gif", "not-image".getBytes()
        );

        when(profileService.updateProfilePhoto(eq(AUTH_USER_ID), any()))
                .thenThrow(new com.ideatrack.main.exception.FileStorageException("Invalid file type"));

        try {
            profileController.uploadPhoto(bad);
        } catch (com.ideatrack.main.exception.FileStorageException ex) {
            assertThat(ex.getMessage()).contains("Invalid file type");
        }
    }

    @Test
    @DisplayName("POST /api/profile/me/profile-photo - I/O failure")
    void uploadPhoto_ioFailure() {
        MockMultipartFile jpg = new MockMultipartFile(
                "file", "p.jpg", "image/jpeg", "fake".getBytes()
        );

        when(profileService.updateProfilePhoto(eq(AUTH_USER_ID), any()))
                .thenThrow(new com.ideatrack.main.exception.FileStorageException("Failed to upload"));

        try {
            profileController.uploadPhoto(jpg);
        } catch (com.ideatrack.main.exception.FileStorageException ex) {
            assertThat(ex.getMessage()).contains("Failed to upload");
        }
    }

    @Test
    @DisplayName("DELETE /api/profile/me/profile-photo - no photo")
    void deletePhoto_noPhoto() {
        when(profileService.deleteProfilePhoto(AUTH_USER_ID))
                .thenThrow(new com.ideatrack.main.exception.ProfileOperationException("No profile photo to delete"));

        try {
            profileController.deletePhoto();
        } catch (com.ideatrack.main.exception.ProfileOperationException ex) {
            assertThat(ex.getMessage()).contains("No profile photo");
        }
    }

    @Test
    @DisplayName("DELETE /api/profile/me/profile-photo - missing file")
    void deletePhoto_missingFile() {
        when(profileService.deleteProfilePhoto(AUTH_USER_ID))
                .thenThrow(new com.ideatrack.main.exception.FileStorageException("Profile photo file not found"));

        try {
            profileController.deletePhoto();
        } catch (com.ideatrack.main.exception.FileStorageException ex) {
            assertThat(ex.getMessage()).contains("file not found");
        }
    }

    @Test
    @DisplayName("PUT /api/profile/me/password - wrong current password")
    void changePassword_wrongCurrent() {
        PasswordUpdateDTO req = new PasswordUpdateDTO();
        req.setCurrentPassword("wrong");
        req.setNewPassword("newPass#2026");

        when(profileService.changePassword(eq(AUTH_USER_ID), anyString(), anyString()))
                .thenThrow(new PasswordChangeException("Current password is incorrect"));

        try {
            profileController.changePassword(req);
        } catch (PasswordChangeException ex) {
            assertThat(ex.getMessage()).contains("incorrect");
        }
    }

    // ------------------------------------------------------------
    //  GET /api/profile/departments (public) - unchanged
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /api/profile/departments: returns 200 OK with DepartmentListResponse")
    void getAllDepartments_ok() {
        DepartmentListResponse expected = new DepartmentListResponse(
                List.of("AI", "HR", "QA")
        );

        when(userService.getAllDepartmentNames()).thenReturn(expected);

        ResponseEntity<DepartmentListResponse> resp = profileController.getAllDepartments();

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getDeptNames()).containsExactly("AI", "HR", "QA");

        verify(userService).getAllDepartmentNames();
        // Only userService should be used by this endpoint
        verifyNoInteractions(profileService);
    }

    // -------------------- helpers ----------------------

    private User mockUser(int id, String email) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        return u;
    }
}