package com.ideatrack.main.controller;

import com.ideatrack.main.dto.profilegamification.IdeaHierarchyDTO;
import com.ideatrack.main.service.HierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hierarchy")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SUPERADMIN','ADMIN','EMPLOYEE','REVIEWER')")
public class HierarchyController {

    private final HierarchyService hierarchyService;

    /**
     * Retrieves the full hierarchy details for a specific idea.
     * Delegates to HierarchyService to build reviewer nodes, admin/owner info,
     * and timeline details before returning the composed DTO.
     */
    @GetMapping("/idea/{ideaId}")
    public ResponseEntity<IdeaHierarchyDTO> getIdeaHierarchy(@PathVariable Integer ideaId)  {
        return ResponseEntity.ok(hierarchyService.getIdeaHierarchy(ideaId));
    }
}
