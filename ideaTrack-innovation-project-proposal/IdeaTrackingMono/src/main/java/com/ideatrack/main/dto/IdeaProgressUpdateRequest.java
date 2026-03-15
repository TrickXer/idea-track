// com/ideatrack/main/dto/idea/IdeaProgressUpdateRequest.java
package com.ideatrack.main.dto;

import com.ideatrack.main.data.Constants;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class IdeaProgressUpdateRequest {
    private Constants.IdeaStatus ideaStatus;  // optional
    private Integer stage;                    // optional
}
