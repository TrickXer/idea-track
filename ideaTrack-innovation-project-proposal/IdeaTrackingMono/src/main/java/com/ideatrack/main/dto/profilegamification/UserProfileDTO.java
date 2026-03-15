package com.ideatrack.main.dto.profilegamification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private Integer userId;
    private String name;
    private String email;
    private String phoneNo;
    private String bio;
    private String profileUrl;
    private String role;
    private String departmentName;
    private Integer totalXP;
    private String level;
    private Integer xpToNextLevel;
    private List<String> badges;
    private boolean profileCompleted;
    private Integer profileCompletionPercent;
}