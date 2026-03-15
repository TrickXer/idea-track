// com/ideatrack/main/dto/common/UserLiteDto.java
package com.ideatrack.main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data 
@Builder
@NoArgsConstructor 
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserLiteDTO {
    private Integer userId;
    private String displayName;   // map from User.getName() or similar
    private String avatarUrl;     // optional
}
