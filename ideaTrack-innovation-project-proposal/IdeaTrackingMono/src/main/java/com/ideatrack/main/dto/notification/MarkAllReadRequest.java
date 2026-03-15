package com.ideatrack.main.dto.notification;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarkAllReadRequest {

    @NotNull
    private Integer userId;
}