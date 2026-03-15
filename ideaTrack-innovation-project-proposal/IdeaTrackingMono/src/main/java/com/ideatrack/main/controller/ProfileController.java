package com.ideatrack.main.controller;

import com.ideatrack.main.config.AuthUtils; // <-- use your existing util
import com.ideatrack.main.dto.DepartmentIDDTOListResponse;
import com.ideatrack.main.dto.DepartmentListResponse;
import com.ideatrack.main.dto.profilegamification.PasswordUpdateDTO;
import com.ideatrack.main.dto.profilegamification.UpdateProfileRequest;
import com.ideatrack.main.dto.profilegamification.UserProfileDTO;
import com.ideatrack.main.repository.IUserRepository;
import com.ideatrack.main.service.ProfileService;
import com.ideatrack.main.service.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','EMPLOYEE','REVIEWER')")
public class ProfileController {

    private final ProfileService profileService;
    private final UserService userService;
    private final IUserRepository userRepository;
    /**
     * Public endpoint: Fetches a user's public profile by path variable userId.
     * Returns a sanitized DTO (no gamification or internal fields).
     */
    @PreAuthorize("permitAll()")
    @GetMapping("/public/{userId}")
    public ResponseEntity<com.ideatrack.main.dto.profilegamification.PublicUserProfileDTO> getPublicProfile(
            @PathVariable Integer userId) {
        return ResponseEntity.ok(profileService.getPublicProfile(userId));
    }

    /**
     * Retrieves a user's profile by userId.
     * Returns a DTO that aggregates identity, contact, department, XP/level/badges, and completion status.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getProfile() {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    /**
     * Updates basic profile attributes (name, phone, bio, profileUrl) when present in the request body.
     * Responds with the updated profile DTO including gamification and completion details.
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @RequestBody UpdateProfileRequest request) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    /**
     * Uploads a profile photo (JPEG/PNG), stores it, and updates the user's profile URL.
     * Responds with the refreshed profile DTO after a successful file write and save.
     */
    @PostMapping("/me/profile-photo")
    public ResponseEntity<UserProfileDTO> uploadPhoto(
            @RequestParam("file") MultipartFile file) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(profileService.updateProfilePhoto(userId, file));
    }

    /**
     * Deletes the user's current profile photo from storage and clears the stored URL.
     * Returns the updated profile DTO after removal.
     */
    @DeleteMapping("/me/profile-photo")
    public ResponseEntity<UserProfileDTO> deletePhoto() {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(profileService.deleteProfilePhoto(userId));
    }

    /**
     * Changes the user's password after validating the current password (if provided) and ensuring the new one differs.
     * Returns a success message when the update completes.
     */
    @PutMapping("/me/password")
    public ResponseEntity<String> changePassword(
            @RequestBody PasswordUpdateDTO request) {
        Integer userId = AuthUtils.currentUserId(userRepository);
        return ResponseEntity.ok(
                profileService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword())
        );
    }

    /**
     * Soft-deletes a user profile by marking it as deleted.
     * Returns HTTP 204 No Content when the operation succeeds.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteProfile() {
        Integer userId = AuthUtils.currentUserId(userRepository);
        profileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("permitAll()")
    @GetMapping("/departments")
    public ResponseEntity<DepartmentListResponse> getAllDepartments() {
        return ResponseEntity.ok(userService.getAllDepartmentNames());
    }
    
    
    @PreAuthorize("permitAll()")
    @GetMapping("/departmentID")
    public ResponseEntity<List<DepartmentIDDTOListResponse>> getAllDepartmentID() {
        return ResponseEntity.ok(userService.getAllDepartmentID());
    }
}