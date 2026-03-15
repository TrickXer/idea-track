// com/ideatrack/main/dto/activity/SaveToggleRequest.java
package com.ideatrack.main.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SaveToggleRequest {
    @NotNull private Integer userId;
    @NotNull private Boolean saved; // true=save, false=unsave
}

