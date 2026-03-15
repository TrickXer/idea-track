package com.ideatrack.main.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectApprovalMetricsDTO {

	private String month;
	private long totalAcceptedIdeaCount;
	private long totalApprovedIdeaCount;
	
}
