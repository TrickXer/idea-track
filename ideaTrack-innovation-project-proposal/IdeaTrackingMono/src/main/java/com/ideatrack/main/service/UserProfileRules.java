package com.ideatrack.main.service;

import org.springframework.stereotype.Component;
import com.ideatrack.main.data.User;

@Component
public class UserProfileRules {

    public boolean isProfileCompleted(User user) {
        return isNonBlank(user.getName())
                && isNonBlank(user.getPhoneNo())
                && isNonBlank(user.getBio())
                && isNonBlank(user.getProfileUrl());
    }

    /**
     * Returns completion percentage (0..100) based on the same fields used
     * by isProfileCompleted(): name, phoneNo, bio, profileUrl.
     */
    public int getProfileCompletionPercent(User user) {
        if (user == null) return 0;

        int total = 4;
        int completed = 0;

        if (isNonBlank(user.getName()))      completed++;
        if (isNonBlank(user.getPhoneNo()))   completed++;
        if (isNonBlank(user.getBio()))       completed++;
        if (isNonBlank(user.getProfileUrl())) completed++;

        // Convert to 0..100 and clamp (defensive)
        int percent = (int) Math.round((completed * 100.0) / total);
        return Math.max(0, Math.min(100, percent));
    }

    private boolean isNonBlank(String s) {
        return s != null && !s.isBlank();
    }
}