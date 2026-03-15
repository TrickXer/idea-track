package com.ideatrack.main.dto.reviewerAssignment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignedReviewerDTO {

	private Integer reviewerId;
	private String name;
    private Integer categoryId;
    private String categoryName;
    private Integer stageNo;
	
}
