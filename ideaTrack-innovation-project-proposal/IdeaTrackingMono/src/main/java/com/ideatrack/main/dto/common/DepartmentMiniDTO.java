package com.ideatrack.main.dto.common;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentMiniDTO {
    private Integer deptId;
    private String deptName;
}