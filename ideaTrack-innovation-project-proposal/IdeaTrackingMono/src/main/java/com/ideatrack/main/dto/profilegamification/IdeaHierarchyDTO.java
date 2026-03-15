package com.ideatrack.main.dto.profilegamification;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class IdeaHierarchyDTO {

    // Idea header
    private Integer ideaId;
    private String title;
    private String description;
    private String status;

    // Reviewer nodes
    private List<HierarchyNodeDTO> nodes;


    // NEW: Sorted ascending by stage
    private Map<Integer, List<HierarchyNodeDTO>> nodesByStage;


    // Grouped admin block
    private AdminInfoDTO admin;

    // NEW: grouped owner block
    private OwnerInfoDTO owner;

    // Timeline
    private List<Map<String, Object>> timeline;

    // Convenience constructor (unchanged)
    public IdeaHierarchyDTO(Integer ideaId,
                            String title,
                            String description,
                            String status,
                            List<HierarchyNodeDTO> nodes) {
        this.ideaId = ideaId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.nodes = nodes;
    }
}