package com.ideatrack.main.dto.reviewerAssignment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableReviewersDTO {

	private Integer userId;
    private String name;
    private String deptName;
	
}
