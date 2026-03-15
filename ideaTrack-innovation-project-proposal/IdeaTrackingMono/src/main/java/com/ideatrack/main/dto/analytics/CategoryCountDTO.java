package com.ideatrack.main.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCountDTO {
	
	private int categoryId;
	private String categoryName;
	private long ideaCount;
	
}
