// com/ideatrack/main/dto/common/ViewerStateDto.java
package com.ideatrack.main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ideatrack.main.data.Constants;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ViewerStatusDTO{
    private Boolean saved;                 // current viewer saved?
    private Constants.VoteType voteType;   // UPVOTE/DOWNVOTE (if any)
    private Boolean owner;                 // viewer is the author?
}
