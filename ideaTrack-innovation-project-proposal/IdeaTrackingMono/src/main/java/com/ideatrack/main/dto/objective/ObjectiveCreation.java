package com.ideatrack.main.dto.objective;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//Done by vibhuti

@Data
public class ObjectiveCreation {
    @NotNull @Min(1)
    private Integer objectiveSeq;

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    private Boolean mandatory;

    @NotNull
    private ProofForObjectiveDTO proof;
}