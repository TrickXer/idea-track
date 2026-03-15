package com.ideatrack.main.config;

import com.ideatrack.main.exception.UnauthorizedException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.IUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthUtils {
    private AuthUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static Integer currentUserId(IUserRepository userRepository) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user");
        }
        String username = auth.getName();
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found: " + username))
                .getUserId();
    }
}