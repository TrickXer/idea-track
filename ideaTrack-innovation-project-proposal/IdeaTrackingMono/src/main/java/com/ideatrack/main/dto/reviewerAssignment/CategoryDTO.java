package com.ideatrack.main.dto.reviewerAssignment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

	private Integer categoryId;
	private String categoryName;
	private int stageCount;
	
}
