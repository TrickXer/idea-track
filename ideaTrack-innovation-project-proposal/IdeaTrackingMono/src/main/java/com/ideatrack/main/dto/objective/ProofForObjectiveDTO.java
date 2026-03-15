package com.ideatrack.main.dto.objective;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

//Done by vibhuti

@Data
public class ProofForObjectiveDTO {

    @NotNull
    private Integer objectiveSeq;   // You may not need this here if you identify objective by id/seq higher up.

    @NotBlank
    private String fileName;

    // Accept only PDF or JPG
    @NotBlank
    private String contentType; // "application/pdf" or "image/jpeg"

    // > 0 and ≤ 25 MB
    @NotNull
    @Positive
    @Max(25L * 1024 * 1024) // 25 MB
    private Long sizeBytes;

    @NotBlank
    private String filePath;  // stored path after upload (use this name)
}