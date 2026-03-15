package com.ideatrack.main.dto.category;

import com.ideatrack.main.dto.common.DepartmentMiniDTO;
import com.ideatrack.main.dto.common.UserMiniDTO;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {
    private Integer categoryId;
    private DepartmentMiniDTO department;   // same name as entity field
    private String name;
    private UserMiniDTO createdByAdmin;     // same name as entity field
    private int reviewerCountPerStage;
    private int stageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}
