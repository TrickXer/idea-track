package com.ideatrack.main.dto.reviewer;


import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * A compact DTO for the UI to render a progression-bar timeline.
 * Nodes (steps) and Edges (pipes) are in the order required by the UI.
 */
@Data
@Builder
public class ProgressionDTO {
    private Integer ideaId;
    private String currentStatus;         // From idea.ideaStatus (enum name)
    private List<Step> steps;             // Nodes in canonical order
    private List<Bar> bars;             // Edges between nodes; 'filled' means the connection is completed

    @Data
    @Builder
    public static class Step {
        private String key;               // SUBMITTED, UNDERREVIEW, REFINE, ACCEPTED, PROJECTPROPOSAL, APPROVED, REJECTED
        private String label;             // UI label (same as key; localize on UI if needed)
        private boolean reached;          // dot filled if true
        private boolean active;           // current node highlight
        private LocalDateTime at;         // when we reached this node (createdAt for SUBMITTED, updatedAt for current node)
    }

    @Data
    @Builder
    public static class Bar {
        private String fromKey;           // e.g., SUBMITTED
        private String toKey;             // e.g., UNDERREVIEW
        private boolean filled;           // pipe filled if true
    }
}