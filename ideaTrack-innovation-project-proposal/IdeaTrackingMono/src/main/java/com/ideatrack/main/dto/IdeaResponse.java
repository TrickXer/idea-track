// com/ideatrack/main/dto/idea/IdeaResponse.java
package com.ideatrack.main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ideatrack.main.data.Constants;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdeaResponse {
	private Integer ideaId;
    private String title;
    private String description;
    private String problemStatement;
    private String tag;
    private String thumbnailURL;
    private CategoryLiteDTO category;
    private UserLiteDTO author;
    private Constants.IdeaStatus ideaStatus;
    private Integer stage;
    
    // ADD THIS FIELD
    private List<String> feedback;

    private VoteCountsDTO votes;
    private long commentsCount;
    private ViewerStatusDTO viewer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}
