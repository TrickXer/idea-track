package com.ideatrack.main.dto.category;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryUpdateRequest {
    private String name;                // optional
    private Integer departmentId;       // optional
    private Integer createdByAdminId;   // optional
    @Min(0)
    private Integer reviewerCountPerStage; // optional
    @Min(1)
    private Integer stageCount;            // optional
}
