package com.ideatrack.main.dto;

import com.ideatrack.main.data.Constants;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Integer userId;
    private String name;
    private String email;
    private String role;
    private Integer deptId;
    private String deptName;
    private String phoneNo;
    private String profileUrl;
    private String bio;
    private Constants.Status status;
    private int totalXP;
    private boolean profileCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}
