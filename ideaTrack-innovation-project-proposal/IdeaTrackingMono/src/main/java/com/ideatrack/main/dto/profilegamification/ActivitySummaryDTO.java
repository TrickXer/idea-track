package com.ideatrack.main.dto.profilegamification;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySummaryDTO {
    private String type;
    private String commentText;
    private int delta;
    private LocalDateTime createdAt;
}
