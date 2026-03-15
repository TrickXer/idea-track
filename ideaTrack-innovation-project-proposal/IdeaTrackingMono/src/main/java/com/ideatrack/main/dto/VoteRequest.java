// com/ideatrack/main/dto/activity/VoteRequest.java
package com.ideatrack.main.dto;

import com.ideatrack.main.data.Constants;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteRequest {
   
    @NotNull private Constants.VoteType voteType; // UPVOTE/DOWNVOTE
}
