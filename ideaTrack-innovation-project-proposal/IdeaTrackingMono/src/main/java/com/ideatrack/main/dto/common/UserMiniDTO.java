package com.ideatrack.main.dto.common;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMiniDTO {
    private Integer userId;
    private String name;
    private String email;
}
