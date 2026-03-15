package com.ideatrack.main.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerPerformanceDTO {
	
	private String month;
	private long assignedIdeaCount;
	private long reviewedOnTimeCount;

}
