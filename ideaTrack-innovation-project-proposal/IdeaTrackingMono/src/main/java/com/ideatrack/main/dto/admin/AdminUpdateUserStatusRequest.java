package com.ideatrack.main.dto.admin;

import com.ideatrack.main.data.Constants;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

//Done by vibhuti

/**
 * Request body for updating a user's status.
 */
@Data
@Builder
public class AdminUpdateUserStatusRequest {

    @NotNull(message = "status is required")
    private Constants.Status status; // ✅ single source of truth (domain enum)
}
