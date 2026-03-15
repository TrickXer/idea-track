package com.ideatrack.main.dto.proposal;


import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ideatrack.main.dto.objective.ObjectiveCreation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

//Done by Vibhuti

@Data
public class ProposalSubmitRequest {

    @NotNull(message = "timeLineStart is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate timeLineStart;

    @NotNull(message = "timeLineEnd is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate timeLineEnd;

    @PositiveOrZero(message = "budget must be ≥ 0")
    private long budget; // required for submit
    

    private List<ObjectiveCreation> objectives;
}
