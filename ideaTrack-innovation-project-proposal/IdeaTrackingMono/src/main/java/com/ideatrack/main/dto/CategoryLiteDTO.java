// com/ideatrack/main/dto/common/CategoryLiteDto.java
package com.ideatrack.main.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data 
@Builder 
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryLiteDTO {
    private Integer categoryId;
    private String name;          // map from Category.getName()
}
