package com.ideatrack.main.dto.analytics;

import com.ideatrack.main.data.Constants;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReportDTO {
	
	private Constants.Scope scope;
	private Integer scopeId;	// Id of the department or category, whose analytics is required.
    private Integer userId;
    private Integer year;
    private Integer month;
	
}

