package com.ideatrack.main.dto.idea.bulk;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkIdeaExportRequest {
    @NotEmpty
    private List<@NotNull Integer> ideaIds;
}
