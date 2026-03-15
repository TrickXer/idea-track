package com.ideatrack.main.dto.idea.bulk;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkIdeaOperationResult {
    private int requestedCount;
    private int foundCount;
    private int updatedCount;

    private List<Integer> updatedIds;
    private List<Integer> notFoundIds;

    private List<String> warnings;
}
