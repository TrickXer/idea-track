package com.ideatrack.main.dto.proposal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProposalListItemResponseDTO {
    private Integer proposalId;
    private Integer ideaId;
    private String  ideaTitle;   // from proposal.getIdea().getTitle()
    private Integer userId;
    private long    budget;

    private LocalDate      timeLineStart;
    private LocalDate      timeLineEnd;
    private String         ideaStatus;
    private LocalDateTime  createdAt;
    private LocalDateTime  updatedAt;
}