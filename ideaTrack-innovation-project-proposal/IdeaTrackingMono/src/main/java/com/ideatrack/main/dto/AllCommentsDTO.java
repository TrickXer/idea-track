package com.ideatrack.main.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllCommentsDTO {
    private String displayName;
    private String commentText;
}