package com.ideatrack.main.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecisionBreakdownDTO {

	private String month;
	private long acceptedCount;
	private long rejectedCount;
	private long reassignCount;	
}
