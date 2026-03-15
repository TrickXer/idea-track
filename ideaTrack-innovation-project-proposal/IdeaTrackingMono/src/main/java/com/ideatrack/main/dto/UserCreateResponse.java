package com.ideatrack.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCreateResponse {
    private String message;
    private UserResponse user;
    private String tempPassword;
}
