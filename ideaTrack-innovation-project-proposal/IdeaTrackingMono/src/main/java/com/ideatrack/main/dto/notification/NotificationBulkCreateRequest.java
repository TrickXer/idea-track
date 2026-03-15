package com.ideatrack.main.dto.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationBulkCreateRequest {

    @NotEmpty
    private List<@Valid NotificationCreateRequest> items;
}