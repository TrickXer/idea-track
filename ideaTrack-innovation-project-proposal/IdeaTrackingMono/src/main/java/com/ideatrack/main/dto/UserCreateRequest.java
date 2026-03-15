package com.ideatrack.main.dto;

import lombok.Data;

@Data
public class UserCreateRequest {
    private String name;
    private String email;
    private String role;      // ADMIN or USER
    private String deptName;
}
