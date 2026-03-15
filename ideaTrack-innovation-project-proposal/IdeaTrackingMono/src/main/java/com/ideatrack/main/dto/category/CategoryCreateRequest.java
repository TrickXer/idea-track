package com.ideatrack.main.dto.category;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCreateRequest {
    @NotBlank
    private String name;

    @NotNull
    private Integer departmentId;

    @NotNull
    private Integer createdByAdminId;

    @Min(0)
    private int reviewerCountPerStage;

    @Min(1)
    private int stageCount;
}
