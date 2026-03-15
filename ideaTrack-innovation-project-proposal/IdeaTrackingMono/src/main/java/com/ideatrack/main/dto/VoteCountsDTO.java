// com/ideatrack/main/dto/common/VoteCountsDto.java
package com.ideatrack.main.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class VoteCountsDTO {
    private long upvotes;
    private long downvotes;
}
