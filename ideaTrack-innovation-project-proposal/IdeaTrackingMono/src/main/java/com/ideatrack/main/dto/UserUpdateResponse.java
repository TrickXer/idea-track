package com.ideatrack.main.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserUpdateResponse {
    private String message;
    private UserResponse user;
}
