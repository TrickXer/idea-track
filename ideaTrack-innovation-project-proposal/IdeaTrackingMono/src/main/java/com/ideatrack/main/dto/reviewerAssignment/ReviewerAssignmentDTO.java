package com.ideatrack.main.dto.reviewerAssignment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewerAssignmentDTO {

    private Integer reviewerId;
    private Integer categoryId;
    private Integer stageNo;
	
}
