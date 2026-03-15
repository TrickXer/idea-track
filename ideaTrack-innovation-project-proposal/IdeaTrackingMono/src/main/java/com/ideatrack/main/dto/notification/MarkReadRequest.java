package com.ideatrack.main.dto.notification;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkReadRequest {

    @NotNull
    private Integer userId;  // ownership enforced

    @NotEmpty
    private List<@NotNull Integer> notificationIds;
}