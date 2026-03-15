package com.ideatrack.main.controller;


import com.ideatrack.main.dto.profilegamification.IdeaHierarchyDTO;
import com.ideatrack.main.exception.IdeaNotFound;
import com.ideatrack.main.service.HierarchyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@WithMockUser(authorities = {"SUPERADMIN", "ADMIN", "EMPLOYEE", "REVIEWER"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestHierarchyController {

    @Autowired
    private HierarchyController hierarchyController;

    @MockitoBean
    private HierarchyService hierarchyService;

    // ------------------------------------------------------------
    // GET /api/hierarchy/idea/{ideaId} - Success
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /api/hierarchy/idea/{ideaId} - Success")
    void getIdeaHierarchy_success() {
        IdeaHierarchyDTO dto = new IdeaHierarchyDTO(
                3017,
                "Process Mining",
                "Process mining",
                "DRAFT",
                java.util.List.of()
        );

        when(hierarchyService.getIdeaHierarchy(eq(3017))).thenReturn(dto);

        ResponseEntity<IdeaHierarchyDTO> response = hierarchyController.getIdeaHierarchy(3017);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIdeaId()).isEqualTo(3017);
        assertThat(response.getBody().getTitle()).isEqualTo("Process Mining");

        Mockito.verify(hierarchyService).getIdeaHierarchy(3017);
    }

    // ------------------------------------------------------------
    // GET /api/hierarchy/idea/{ideaId} -> throws IdeaNotFound
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /api/hierarchy/idea/{ideaId} -> throws IdeaNotFound")
    void getIdeaHierarchy_ideaNotFound() {
        when(hierarchyService.getIdeaHierarchy(eq(9999)))
                .thenThrow(new IdeaNotFound("Idea not found"));

        try {
            hierarchyController.getIdeaHierarchy(9999);
            org.junit.jupiter.api.Assertions.fail("Expected IdeaNotFound to be thrown");
        } catch (IdeaNotFound ex) {
            assertThat(ex.getMessage()).isEqualTo("Idea not found");
        }
    }

    // ------------------------------------------------------------
    // GET /api/hierarchy/idea/{ideaId} -> throws IllegalArgumentException (optional)
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /api/hierarchy/idea/{ideaId} -> throws IllegalArgumentException for invalid id")
    void getIdeaHierarchy_invalidId() {
        when(hierarchyService.getIdeaHierarchy(eq(-1)))
                .thenThrow(new IllegalArgumentException("Idea not found"));

        try {
            hierarchyController.getIdeaHierarchy(-1);
            org.junit.jupiter.api.Assertions.fail("Expected IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("Idea not found");
        }
    }
}