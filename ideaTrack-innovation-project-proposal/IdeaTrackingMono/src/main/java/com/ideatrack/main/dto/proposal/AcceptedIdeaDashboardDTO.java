package com.ideatrack.main.dto.proposal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AcceptedIdeaDashboardDTO {

	 private Integer ideaId;
	 private String ideaTitle;
	 private String ideaDescription;
	 private String ideaStatus;
	 private LocalDateTime ideaCreatedAt;
	
	 private Integer proposalId;
	 private Long budget;
	 private LocalDate timeLineStart;
	 private LocalDate timeLineEnd;
	 private String proposalStatus;
	 private LocalDateTime proposalCreatedAt;

}