package com.ideatrack.main.dto.proposal;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ideatrack.main.dto.objective.ObjectiveCreation;

//Done by Vibhuti

@Data
public class ProposalUpdateRequestDTO {

	    private Long budget;

	    @JsonFormat(pattern = "yyyy-MM-dd")
	    private LocalDate timeLineStart;

	    @JsonFormat(pattern = "yyyy-MM-dd")
	    private LocalDate timeLineEnd;

	    @JsonProperty("objectives")
	    private List<ObjectiveCreation> objectives;

	}