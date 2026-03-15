package com.ideatrack.main.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentMetricsDTO {
	
	private String deptName;
    private long ideaCount;
    private long voteCount;
    private long commentCount;
}
