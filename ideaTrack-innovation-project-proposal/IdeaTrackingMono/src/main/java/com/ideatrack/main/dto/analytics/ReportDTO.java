package com.ideatrack.main.dto.analytics;

import java.time.LocalDateTime;

import com.ideatrack.main.data.Constants.Scope;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

	private Integer id;
	private Scope scope;
    private String dataOf;
    private int ideasSubmitted;
    private int approvedCount;
    private int participationCount;
    private String userName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
