// com/ideatrack/main/dto/idea/IdeaUpdateRequest.java
package com.ideatrack.main.dto;

import lombok.*;

@Data 
@NoArgsConstructor
@AllArgsConstructor 
@Builder
public class IdeaUpdateRequest {
    private String title;
    private String description;
    private String problemStatement;
    private Integer categoryId;
    private String tag;
    private String thumbnailURL;
                      
}
