package com.ideatrack.main.dto.profilegamification;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String name;
    private String phoneNo;
    private String bio;
    private String profileUrl;
}
