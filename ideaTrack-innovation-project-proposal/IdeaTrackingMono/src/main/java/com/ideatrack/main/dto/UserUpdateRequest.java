package com.ideatrack.main.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String email;
    private String role;
    private String deptName;
    private String phoneNo;
    private String bio;
    private String profileUrl;
}
