package com.ideatrack.main.dto.objective;

import lombok.*;

//Done by vibhuti

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectivesResponse {
   
    private Long id;                // <-- must exist if you call .id(...)
    private Integer objectiveSeq;
    private String title;
    private String description;
    private boolean mandatory;
    private String proofFileName;
    private String proofContentType;
    private String proofFilePath;
    private Long proofSizeBytes;
}
