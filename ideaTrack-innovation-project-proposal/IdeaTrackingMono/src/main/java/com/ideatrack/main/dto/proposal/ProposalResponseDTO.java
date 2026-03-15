package com.ideatrack.main.dto.proposal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ideatrack.main.data.Objectives;
import com.ideatrack.main.dto.objective.ObjectiveCreation;

import lombok.Data;

//Done by Vibhuti

/**
 * Returned by: convert-to-proposal, updateDraft, submit, and future get-by-id.
 */
//com.ideatrack.main.dto.proposal.ProposalResponseDTO
@Data
public class ProposalResponseDTO {

 private Integer proposalId;
 private Integer ideaId;
 private Integer userId;

 private long budget;

 // ❌ private List<Objectives> objective;
 // ✅ Use a DTO instead (you already have ObjectivesResponse)
 private List<com.ideatrack.main.dto.objective.ObjectivesResponse> objective;

 @JsonProperty("timeLineStart")
 private LocalDate timeLineStart;

 @JsonProperty("timeLineEnd")
 private LocalDate timeLineEnd;

 private String ideaStatus;
 private LocalDateTime createdAt;
 private LocalDateTime updatedAt;
}