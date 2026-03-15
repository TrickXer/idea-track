// com/ideatrack/main/dto/activity/ActivityResultResponse.java
package com.ideatrack.main.dto;

import com.ideatrack.main.data.Constants;

import lombok.*;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class ActivityResultResponse {
    private Integer ideaId;
    private Integer userId;

    // For votes (nullable if not a vote action)
    private Constants.VoteType voteType;
    private VoteCountsDTO votes;

    // For saves (nullable if not a  save action)
    private Boolean saved;
}
