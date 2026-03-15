package com.ideatrack.main.dto.reviewer;

import lombok.Data;

@Data
public class ProposalDecisionRequest {
	private String decision;
    private Integer userId;       // same as reviewerId, but generic for author
    private String text;          // the comment content


}