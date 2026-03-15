package com.ideatrack.main.service;

import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.profilegamification.PublicUserProfileDTO;
import com.ideatrack.main.dto.profilegamification.UpdateProfileRequest;
import com.ideatrack.main.dto.profilegamification.UserProfileDTO;
import com.ideatrack.main.exception.*;
import com.ideatrack.main.repository.IUserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfileService covering happy paths and error cases.
 */
@ExtendWith(MockitoExtension.class)
class TestProfileService {

    @Mock private IUserRepository userRepository;
    @Mock private GamificationService gamificationService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private UserProfileRules profileRules;

    @InjectMocks private ProfileService profileService;

    private User user;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(Paths.get("uploads/profile-pics"));

        // Minimal user
        user = new User();
        user.setUserId(1013);
        user.setName("Employee 1");
        user.setEmail("emp1@company.com");
        user.setPhoneNo("+91-9000000001");
        user.setPassword("$2a$hash");
        user.setTotalXP(120);
        user.setBio("Engineer");
        user.setProfileUrl(null);
        user.setDeleted(false);

        // Stubbing used by DTO mapping in most profile-returning methods
        lenient().when(gamificationService.calculateLevel(anyInt())).thenReturn("Silver");
        lenient().when(gamificationService.computeXpToNextLevel(anyInt())).thenReturn(80);
        lenient().when(gamificationService.calculateBadges(any()))
                .thenReturn(List.of("Spark Igniter"));
        lenient().when(profileRules.isProfileCompleted(any())).thenReturn(true);
    }

    // ------------------------------------------------------------
    // getPublicProfile (NEW TESTS)
    // ------------------------------------------------------------
    @Test
    @DisplayName("getPublicProfile: returns sanitized DTO with totalXp and string level")
    void getPublicProfile_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        when(gamificationService.calculateLevel(120)).thenReturn("Silver");

        PublicUserProfileDTO dto = profileService.getPublicProfile(1013);

        assertThat(dto.getUserId()).isEqualTo(1013);
        assertThat(dto.getName()).isEqualTo("Employee 1");
        assertThat(dto.getEmail()).isEqualTo("emp1@company.com");
        assertThat(dto.getPhoneNo()).isEqualTo("+91-9000000001");
        assertThat(dto.getBio()).isEqualTo("Engineer");
        assertThat(dto.getProfileUrl()).isNull();
        assertThat(dto.getRole()).isNull(); // not set in setup user
        assertThat(dto.getDepartmentName()).isNull();

        // Newly added assertions
        assertThat(dto.getTotalXp()).isEqualTo(120);
        assertThat(dto.getLevel()).isEqualTo("Silver");

        verify(userRepository).findById(1013);
        verify(gamificationService).calculateLevel(120);
    }

    @Test
    @DisplayName("getPublicProfile: throws UserNotFound when user missing")
    void getPublicProfile_notFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getPublicProfile(999))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("getPublicProfile: soft-deleted user -> UserNotFound")
    void getPublicProfile_softDeleted() {
        user.setDeleted(true);
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> profileService.getPublicProfile(1013))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ------------------------------------------------------------
    // getProfile
    // ------------------------------------------------------------
    @Test
    @DisplayName("getProfile: returns DTO when user exists")
    void getProfile_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        UserProfileDTO dto = profileService.getProfile(1013);

        assertThat(dto.getUserId()).isEqualTo(1013);
        assertThat(dto.getName()).isEqualTo("Employee 1");
        assertThat(dto.getLevel()).isEqualTo("Silver");
        assertThat(dto.getXpToNextLevel()).isEqualTo(80);
        verify(userRepository).findById(1013);
    }

    @Test
    @DisplayName("getProfile: throws UserNotFound when user is missing")
    void getProfile_notFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> profileService.getProfile(999))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ------------------------------------------------------------
    // updateProfile
    // ------------------------------------------------------------
    @Test
    @DisplayName("updateProfile: updates provided fields and returns DTO")
    void updateProfile_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("Emp One");
        req.setPhoneNo("+91-9999999999");
        req.setBio("Updated bio");
        req.setProfileUrl("/some/url.png");

        UserProfileDTO dto = profileService.updateProfile(1013, req);

        assertThat(dto.getName()).isEqualTo("Emp One");
        assertThat(dto.getPhoneNo()).isEqualTo("+91-9999999999");
        assertThat(dto.getBio()).isEqualTo("Updated bio");
        assertThat(dto.getProfileUrl()).isEqualTo("/some/url.png");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updateProfile: throws UserNotFound when user missing")
    void updateProfile_notFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> profileService.updateProfile(999, new UpdateProfileRequest()))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("updateProfile: empty name -> ProfileOperationException")
    void updateProfile_emptyName() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setName("   "); // trims to empty
        assertThatThrownBy(() -> profileService.updateProfile(1013, req))
                .isInstanceOf(ProfileOperationException.class)
                .hasMessageContaining("Name cannot be empty");
    }

    @Test
    @DisplayName("updateProfile: invalid phone format -> ProfileOperationException")
    void updateProfile_invalidPhoneFormat() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhoneNo("abc123"); // fails ^[+()\-\\s\\d]{7,20}$

        assertThatThrownBy(() -> profileService.updateProfile(1013, req))
                .isInstanceOf(ProfileOperationException.class)
                .hasMessageContaining("Phone number format is invalid");
    }

    @Test
    @DisplayName("updateProfile: empty phone -> ProfileOperationException")
    void updateProfile_emptyPhone() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setPhoneNo("   "); // trims to empty

        assertThatThrownBy(() -> profileService.updateProfile(1013, req))
                .isInstanceOf(ProfileOperationException.class)
                .hasMessageContaining("Phone number cannot be empty");
    }

    @Test
    @DisplayName("updateProfile: empty bio is stored as empty string")
    void updateProfile_emptyBioToEmptyString() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        user.setBio("Old bio");

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setBio("   ");

        UserProfileDTO dto = profileService.updateProfile(1013, req);

        assertThat(dto.getBio()).isNotNull();
        assertThat(dto.getBio()).isEmpty();
        assertThat(user.getBio()).isEmpty();
        verify(userRepository).save(user);
    }


    @Test
    @DisplayName("updateProfile: empty profileUrl is stored as null")
    void updateProfile_emptyProfileUrlToNull() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setProfileUrl("   "); // becomes null in service

        UserProfileDTO dto = profileService.updateProfile(1013, req);
        assertThat(dto.getProfileUrl()).isNull();
        verify(userRepository).save(user);
    }

    // ------------------------------------------------------------
    // deleteProfile
    // ------------------------------------------------------------
    @Test
    @DisplayName("deleteProfile: sets deleted flag and saves")
    void deleteProfile_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        profileService.deleteProfile(1013);

        assertThat(user.isDeleted()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteProfile: throws UserNotFound when user missing")
    void deleteProfile_notFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> profileService.deleteProfile(999))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ------------------------------------------------------------
    // updateProfilePhoto
    // ------------------------------------------------------------
    @Test
    @DisplayName("updateProfilePhoto: saves JPEG and updates profileUrl")
    void updateProfilePhoto_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake".getBytes()
        );

        UserProfileDTO dto = profileService.updateProfilePhoto(1013, file);

        assertThat(dto.getProfileUrl()).contains("/uploads/profile-pics/1013_profile.jpg");
        verify(userRepository).save(user);
        assertThat(Files.exists(Paths.get("uploads/profile-pics/1013_profile.jpg"))).isTrue();
    }

    @Test
    @DisplayName("updateProfilePhoto: rejects invalid content type")
    void updateProfilePhoto_invalidType() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.gif", "image/gif", "fake".getBytes()
        );

        assertThatThrownBy(() -> profileService.updateProfilePhoto(1013, file))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("updateProfilePhoto: rejects invalid extension")
    void updateProfilePhoto_invalidExtension() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.gif", "image/jpeg", "fake".getBytes()
        );

        assertThatThrownBy(() -> profileService.updateProfilePhoto(1013, file))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Invalid file extension");
    }

    @Test
    @DisplayName("updateProfilePhoto: rejects files > 5MB")
    void updateProfilePhoto_tooLarge() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        byte[] big = new byte[(int)(5L * 1024 * 1024) + 1]; // 5 MB + 1 byte
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", big
        );

        assertThatThrownBy(() -> profileService.updateProfilePhoto(1013, file))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("File too large");
    }

    @Test
    @DisplayName("updateProfilePhoto: deletes old photo if different target path")
    void updateProfilePhoto_deletesOldDifferentFile() throws IOException {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        // Seed an existing .jpg
        Path oldPath = Paths.get("uploads/profile-pics/1013_profile.jpg");
        Files.write(oldPath, "old".getBytes());
        user.setProfileUrl("/" + oldPath.toString());

        // Upload a .png => new target will be 1013_profile.png (different from old .jpg)
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.png", "image/png", "newcontent".getBytes()
        );

        UserProfileDTO dto = profileService.updateProfilePhoto(1013, file);

        assertThat(dto.getProfileUrl()).contains("/uploads/profile-pics/1013_profile.png");
        assertThat(Files.exists(oldPath)).isFalse(); // old file deleted
        assertThat(Files.exists(Paths.get("uploads/profile-pics/1013_profile.png"))).isTrue();
    }

    @Test
    @DisplayName("updateProfilePhoto: IO failure surfaces as FileStorageException")
    void updateProfilePhoto_ioFailure() throws IOException {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        // Valid content type & name, but getBytes() throws IOException to simulate disk failure
        org.springframework.web.multipart.MultipartFile mf = mock(org.springframework.web.multipart.MultipartFile.class);
        when(mf.getContentType()).thenReturn("image/jpeg");
        when(mf.getOriginalFilename()).thenReturn("photo.jpg");
        when(mf.getBytes()).thenThrow(new IOException("disk full"));

        assertThatThrownBy(() -> profileService.updateProfilePhoto(1013, mf))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Failed to upload profile photo");
    }

    // ------------------------------------------------------------
    // deleteProfilePhoto
    // ------------------------------------------------------------
    @Test
    @DisplayName("deleteProfilePhoto: deletes file and clears profileUrl")
    void deleteProfilePhoto_ok() throws IOException {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        Path p = Paths.get("uploads/profile-pics/1013_profile.png");
        Files.write(p, "fake".getBytes());
        user.setProfileUrl("/" + p.toString());

        UserProfileDTO dto = profileService.deleteProfilePhoto(1013);

        assertThat(dto.getProfileUrl()).isNull();
        assertThat(Files.exists(p)).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteProfilePhoto: no profile photo set -> ProfileOperationException")
    void deleteProfilePhoto_noPhoto() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        user.setProfileUrl(null);

        assertThatThrownBy(() -> profileService.deleteProfilePhoto(1013))
                .isInstanceOf(ProfileOperationException.class)
                .hasMessageContaining("No profile photo to delete");
    }

    @Test
    @DisplayName("deleteProfilePhoto: invalid delete path -> FileStorageException")
    void deleteProfilePhoto_invalidPath() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        // Will normalize to a path that doesn't start with uploads/profile-pics
        user.setProfileUrl("/etc/passwd");

        assertThatThrownBy(() -> profileService.deleteProfilePhoto(1013))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Invalid file path");
    }

    @Test
    @DisplayName("deleteProfilePhoto: missing file on disk -> FileStorageException")
    void deleteProfilePhoto_missingFile() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        user.setProfileUrl("/uploads/profile-pics/does-not-exist.png");

        assertThatThrownBy(() -> profileService.deleteProfilePhoto(1013))
                .isInstanceOf(FileStorageException.class)
                .hasMessageContaining("Profile photo file not found");
    }

    // ------------------------------------------------------------
    // changePassword
    // ------------------------------------------------------------
    @Test
    @DisplayName("changePassword: happy path with strength validation")
    void changePassword_ok() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        // current password provided and correct
        when(passwordEncoder.matches("oldPass#1", user.getPassword())).thenReturn(true);
        // new password must differ
        when(passwordEncoder.matches("NewPass#2026", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewPass#2026")).thenReturn("$enc");

        String msg = profileService.changePassword(1013, "oldPass#1", "NewPass#2026");

        assertThat(msg).contains("Password changed successfully");
        assertThat(user.getPassword()).isEqualTo("$enc");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword: wrong current password -> PasswordChangeException")
    void changePassword_wrongCurrent() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(1013, "wrong", "NewPass#2026"))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    @DisplayName("changePassword: new password equals current -> PasswordChangeException")
    void changePassword_newSameAsCurrent() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));

        String currentRaw = "OldPass#123";
        String newRawSame = "Same#123A";

        // 1) current password verification (must pass)
        when(passwordEncoder.matches(eq(currentRaw), eq(user.getPassword()))).thenReturn(true);
        // 2) new password equals current? (trigger the "same password" branch)
        when(passwordEncoder.matches(eq(newRawSame), eq(user.getPassword()))).thenReturn(true);

        assertThatThrownBy(() -> profileService.changePassword(1013, currentRaw, newRawSame))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessageContaining("New password cannot be the same");
    }

    @Test
    @DisplayName("changePassword: too short -> PasswordChangeException")
    void changePassword_tooShort() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(1013, null, "Ab#1")) // 4 chars
                .isInstanceOf(PasswordChangeException.class)
                .hasMessageContaining("at least 8");
    }

    @Test
    @DisplayName("changePassword: weak pattern -> PasswordChangeException")
    void changePassword_weakPattern() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(1013, null, "alllowercase#"))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessageContaining("Password too weak");
    }

    @Test
    @DisplayName("changePassword: current password null is allowed when new is strong")
    void changePassword_currentNullAllowed() {
        when(userRepository.findById(1013)).thenReturn(Optional.of(user));
        // ensure "new != current"
        when(passwordEncoder.matches("Strong#Pass1", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("Strong#Pass1")).thenReturn("$encoded");

        String msg = profileService.changePassword(1013, null, "Strong#Pass1");
        assertThat(msg).contains("Password changed successfully");
        assertThat(user.getPassword()).isEqualTo("$encoded");
        verify(userRepository).save(user);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Cleanup test files (best-effort)
        Path dir = Paths.get("uploads/profile-pics");
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.compareTo(a)) // delete children first
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                        });
            }
        }
    }
}