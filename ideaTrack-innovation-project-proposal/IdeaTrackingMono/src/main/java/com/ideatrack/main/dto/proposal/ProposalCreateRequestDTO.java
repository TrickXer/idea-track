package com.ideatrack.main.dto.proposal;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ideatrack.main.dto.objective.ObjectiveCreation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

//Done by Vibhuti

@Data
public class ProposalCreateRequestDTO {

    @NotNull(message = "userId is required")
    private Integer userId;

    /** Optional: if provided, we will create these objectives atomically with the proposal */
    @Valid
    private List<ObjectiveCreation> objectives;

    @PositiveOrZero(message = "budget must be ≥ 0")
    private long budget; // required for submit
    
    @NotNull(message = "timeLineStart is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate timeLineStart;

    @NotNull(message = "timeLineEnd is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate timeLineEnd;
    /**
     * Optional: proofs to attach to the objectives created in the same request.
     * Each proof is matched by objectiveSeq; proofs cannot target pre-existing objectives in this flow.
     */

}