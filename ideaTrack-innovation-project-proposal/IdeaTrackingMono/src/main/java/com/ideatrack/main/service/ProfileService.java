package com.ideatrack.main.service;

import com.ideatrack.main.dto.profilegamification.PublicUserProfileDTO;
import com.ideatrack.main.dto.profilegamification.UpdateProfileRequest;
import com.ideatrack.main.dto.profilegamification.UserProfileDTO;
import com.ideatrack.main.data.User;
import com.ideatrack.main.repository.IUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.file.*;

import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.exception.FileStorageException;
import com.ideatrack.main.exception.ProfileOperationException;
import com.ideatrack.main.exception.PasswordChangeException;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final IUserRepository userRepository;
    private final GamificationService gamificationService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserProfileRules profileRules;
    
    private static final String ERROR_USER_NOT_FOUND = "User not found";

    private static final String UPLOAD_DIR = "uploads/profile-pics/";

    /**
     * Returns a public (sanitized) profile DTO for a given user.
     * Excludes gamification and internal flags. 404 if user not found or soft-deleted.
     */
    public PublicUserProfileDTO getPublicProfile(Integer userId) {
        log.info("Fetching PUBLIC profile for userId={}", userId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Public profile: user not found, userId={}", userId);
                    return new UserNotFoundException(ERROR_USER_NOT_FOUND);
                });

        if (user.isDeleted()) {
            log.warn("Public profile: user is deleted, userId={}", userId);
            throw new UserNotFoundException(ERROR_USER_NOT_FOUND);
        }

        return toPublicUserProfileDTO(user);
    }

    private PublicUserProfileDTO toPublicUserProfileDTO(User user) {

        return PublicUserProfileDTO.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNo(user.getPhoneNo())
                .bio(user.getBio())
                .profileUrl(user.getProfileUrl())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getDeptName() : null)
                .totalXp(user.getTotalXP())
                .level(gamificationService.calculateLevel(user.getTotalXP()))
                .build();
    }



    /**
     * Retrieves a user's profile by ID and maps it to a DTO.
     * Throws when the user is not found.
     */
    public UserProfileDTO getProfile(Integer userId) {
        log.info("Fetching profile for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found, userId={}", userId);
                    return new UserNotFoundException(ERROR_USER_NOT_FOUND);
                });
        return toUserProfileDTO(user);
    }



    /**
     * Updates provided profile fields with validation for name and phone.
     * Persists the changes and returns the updated DTO.
     */
    public UserProfileDTO updateProfile(Integer userId, UpdateProfileRequest request) {
        log.info("Updating profile for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for update, userId={}", userId);
                    return new UserNotFoundException(ERROR_USER_NOT_FOUND);
                });

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                log.warn("Empty name provided for userId={}", userId);
                throw new ProfileOperationException("Name cannot be empty");
            }
            user.setName(name);
            log.debug("Updated name for userId={}", userId);
        }

        if (request.getPhoneNo() != null) {
            String phone = request.getPhoneNo().trim();
            if (phone.isEmpty()) {
                log.warn("Empty phone number for userId={}", userId);
                throw new ProfileOperationException("Phone number cannot be empty");
            }
            if (!phone.matches("^[+()\\-\\s\\d]{7,20}$")) {
                log.warn("Invalid phone number format for userId={}", userId);
                throw new ProfileOperationException("Phone number format is invalid");
            }
            user.setPhoneNo(phone);
            log.debug("Updated phone number for userId={}", userId);
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
            log.debug("Updated bio for userId={}", userId);
        }

        if (request.getProfileUrl() != null) {
            user.setProfileUrl(request.getProfileUrl().trim().isEmpty() ? null : request.getProfileUrl().trim());
            log.debug("Updated profile URL for userId={}", userId);
        }

        userRepository.save(user);
        return toUserProfileDTO(user);
    }

    /**
     * Marks a user as deleted without physical removal.
     * Saves the deletion flag and returns.
     */
    public void deleteProfile(Integer userId) {
        log.info("Deleting (soft) profile for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for deletion, userId={}", userId);
                    return new UserNotFoundException(ERROR_USER_NOT_FOUND);
                });
        user.setDeleted(true);
        userRepository.save(user);
        log.info("Profile soft-deleted for userId={}", userId);
    }

    /**
     * Validates and stores a new profile photo with safe file handling.
     * Replaces existing photo if present and updates the profile URL.
     */
    public UserProfileDTO updateProfilePhoto(Integer userId, MultipartFile file) {
        log.info("Updating profile photo for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for photo upload, userId={}", userId);
                    return new UserNotFoundException(ERROR_USER_NOT_FOUND);
                });

        validateContentType(file, userId);

        long maxBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            log.warn("File too large for userId={}, size={} bytes", userId, file.getSize());
            throw new FileStorageException("File too large. Max 5 MB allowed.");
        }

        try {
            Path uploadPath = ensureUploadDir();
            String extension = validateAndExtractExtension(file.getOriginalFilename(), userId);
            Path targetPath = resolveTargetPath(uploadPath, userId, extension);

            deleteOldPhotoIfPresent(user, uploadPath, targetPath, userId);
            writeFileAtomically(uploadPath, targetPath, file, extension);

            user.setProfileUrl("/" + UPLOAD_DIR + userId + "_profile" + extension);
            userRepository.save(user);

            log.info("Profile photo updated successfully for userId={}", userId);
            return toUserProfileDTO(user);

        } catch (IOException e) {
            log.error("Failed to upload profile photo for userId={}", userId, e);
            throw new FileStorageException("Failed to upload profile photo");
        }
    }

    private void validateContentType(MultipartFile file, Integer userId) {
        String contentType = file.getContentType();
        if (contentType == null ||
                !(contentType.equalsIgnoreCase("image/jpeg")
                        || contentType.equalsIgnoreCase("image/jpg")
                        || contentType.equalsIgnoreCase("image/png"))) {
            log.warn("Invalid file type for userId={}, contentType={}", userId, contentType);
            throw new FileStorageException("Invalid file type. Only JPEG and PNG are allowed.");
        }
    }

    private String validateAndExtractExtension(String originalName, Integer userId) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!(extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".png"))) {
            log.warn("Invalid file extension '{}' for userId={}", extension, userId);
            throw new FileStorageException("Invalid file extension. Only .jpg, .jpeg, .png are allowed.");
        }
        return extension;
    }

    private Path ensureUploadDir() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR).normalize().toAbsolutePath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath);
        }
        return uploadPath;
    }

    private Path resolveTargetPath(Path uploadPath, Integer userId, String extension) {
        String fileName = userId + "_profile" + extension;
        Path targetPath = uploadPath.resolve(fileName).normalize().toAbsolutePath();
        if (!targetPath.startsWith(uploadPath)) {
            log.error("Invalid file path attempt for userId={}", userId);
            throw new FileStorageException("Invalid file path.");
        }
        return targetPath;
    }

    private void deleteOldPhotoIfPresent(User user, Path uploadPath, Path targetPath, Integer userId) {
        String oldUrl = user.getProfileUrl();
        if (oldUrl == null) {
            return;
        }

        String relative = oldUrl.startsWith("/") ? oldUrl.substring(1) : oldUrl;
        Path oldPath = Paths.get(relative).normalize().toAbsolutePath();

        if (oldPath.startsWith(uploadPath) && !oldPath.equals(targetPath) && Files.exists(oldPath)) {
            try {
                Files.delete(oldPath);
                log.info("Deleted old profile photo for userId={}", userId);
            } catch (IOException ex) {
                log.error("Failed to delete old photo for userId={}", userId, ex);
                throw new FileStorageException("Failed to delete old profile photo");
            }
        }
    }

    private void writeFileAtomically(Path uploadPath, Path targetPath, MultipartFile file, String extension) throws IOException {
        Path temp = Files.createTempFile(uploadPath, "upload_", extension);
        Files.write(temp, file.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        Files.move(temp, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Deletes the current profile photo from disk and clears its URL.
     * Validates safe paths and handles missing files explicitly.
     */
    public UserProfileDTO deleteProfilePhoto(Integer userId) {
        log.info("Deleting profile photo for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for photo deletion, userId={}", userId);
                    return new UserNotFoundException(ERROR_USER_NOT_FOUND);
                });

        if (user.getProfileUrl() != null) {
            String relativePath = user.getProfileUrl().startsWith("/")
                    ? user.getProfileUrl().substring(1)
                    : user.getProfileUrl();

            Path uploadPath = Paths.get(UPLOAD_DIR).normalize().toAbsolutePath();
            Path filePath = Paths.get(relativePath).normalize().toAbsolutePath();

            if (!filePath.startsWith(uploadPath)) {
                log.error("Invalid delete path for userId={}, file={}", userId, filePath);
                throw new FileStorageException("Invalid file path.");
            }

            try {
                Files.delete(filePath);
                log.info("Profile photo deleted for userId={}", userId);
            } catch (NoSuchFileException e) {
                log.error("Photo file not found for userId={}, file={}", userId, filePath);
                throw new FileStorageException("Profile photo file not found: " + filePath);
            } catch (IOException e) {
                log.error("Failed to delete photo for userId={}", userId, e);
                throw new FileStorageException("Failed to delete profile photo file: " + filePath);
            }

        } else {
            log.warn("Attempt to delete non-existing photo for userId={}", userId);
            throw new ProfileOperationException("No profile photo to delete");
        }

        user.setProfileUrl(null);
        userRepository.save(user);

        return toUserProfileDTO(user);
    }

    /**
     * Changes the user's password with checks for correctness and strength.
     * Rejects same-as-old, too short, and weak passwords.
     */
    public String changePassword(Integer userId, String currentPassword, String newPassword) {
        log.info("Changing password for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found for password change, userId={}", userId);
                    return new UserNotFoundException(ERROR_USER_NOT_FOUND);
                });

        if (currentPassword != null) {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                log.warn("Incorrect current password for userId={}", userId);
                throw new PasswordChangeException("Current password is incorrect");
            }
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("New password matches old password for userId={}", userId);
            throw new PasswordChangeException("New password cannot be the same as the current password");
        }

        if (newPassword == null || newPassword.length() < 8) {
            log.warn("Weak password entered for userId={}", userId);
            throw new PasswordChangeException("Password must be at least 8 characters long.");
        }

        String passwordRegex =
                "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

        if (!newPassword.matches(passwordRegex)) {
            log.warn("Password does not meet complexity rules for userId={}", userId);
            throw new PasswordChangeException(
                    "Password too weak! Must contain at least one uppercase, one lowercase, one digit, and one special character (@#$%^&+=!)."
            );
        }

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedNewPassword);
        userRepository.save(user);

        log.info("Password successfully changed for userId={}", userId);
        return "Password changed successfully";
    }

    /**
     * Maps a User entity to a profile DTO enriched with gamification data.
     * Includes level, XP to next level, badges, and completion status.
     */
    private UserProfileDTO toUserProfileDTO(User user) {
        return new UserProfileDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNo(),
                user.getBio(),
                user.getProfileUrl(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getDepartment() != null ? user.getDepartment().getDeptName() : null,
                user.getTotalXP(),
                gamificationService.calculateLevel(user.getTotalXP()),
                gamificationService.computeXpToNextLevel(user.getTotalXP()),
                gamificationService.calculateBadges(user),
                profileRules.isProfileCompleted(user),
                profileRules.getProfileCompletionPercent(user)
        );
    }
}