package com.ideatrack.main.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
    private String name;
    private String role;
    private String deptName;
}
