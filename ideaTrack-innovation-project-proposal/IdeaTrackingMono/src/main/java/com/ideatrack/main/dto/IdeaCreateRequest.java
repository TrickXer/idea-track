// com/ideatrack/main/dto/idea/IdeaCreateRequest.java
package com.ideatrack.main.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class IdeaCreateRequest {
    @NotBlank private String title;
    private String description;
    private String problemStatement;
    @NotNull  private Integer categoryId;
    private String tag;
    private String thumbnailURL;
}
