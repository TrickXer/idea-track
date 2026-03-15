package com.ideatrack.main.dto.profilegamification;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PasswordUpdateDTO {
    private String currentPassword;
    private String newPassword;
}