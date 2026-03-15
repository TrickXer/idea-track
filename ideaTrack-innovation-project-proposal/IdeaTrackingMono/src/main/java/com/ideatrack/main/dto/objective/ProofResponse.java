package com.ideatrack.main.dto.objective;

import lombok.*;

//Done by vibhuti

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofResponse {
    private String fileName;
    private String filePath;
    private String contentType;
    private long sizeBytes;
}
