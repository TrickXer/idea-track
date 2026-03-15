package com.ideatrack.main.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ideatrack.main.dto.reviewerAssignment.AssignedReviewerDTO;
import com.ideatrack.main.dto.reviewerAssignment.AvailableReviewersDTO;
import com.ideatrack.main.dto.reviewerAssignment.CategoryDTO;
import com.ideatrack.main.dto.reviewerAssignment.ReviewerAssignmentDTO;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.service.ReviewerStageAssignmentService;

@SpringBootTest
@WithMockUser(authorities = {"ADMIN", "SUPERADMIN"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestReviewerStageAssignmentController {

    @Autowired
    private ReviewerStageAssignmentController controller;

    @MockitoBean
    private ReviewerStageAssignmentService service;

    
    // ------------------------------------------------------------
    // 1) GET /getAvailableReviewersList/{deptId}
    // ------------------------------------------------------------
    
    @Test
    @DisplayName("GET Available Reviewers - Success")
    void getAvailableReviewersList_ok() {
        AvailableReviewersDTO dto = new AvailableReviewersDTO(101, "Reviewer One", "IT Dept");
        when(service.getAvailableReviewersList(1)).thenReturn(List.of(dto));

        ResponseEntity<List<AvailableReviewersDTO>> resp = controller.getAvailableReviewersList(1);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getName()).isEqualTo("Reviewer One");
    }

    
    // ------------------------------------------------------------
    // 2) GET /getCategoriesAndStageCountByCategory/{deptId}
    // ------------------------------------------------------------
    
    @Test
    @DisplayName("GET Categories & Stage Counts - Success")
    void getCategoriesAndStageCountByCategory_ok() {
        CategoryDTO dto = new CategoryDTO(501, "Java Development", 3);
        when(service.getCategoriesAndStageCountByCategory(1)).thenReturn(List.of(dto));

        ResponseEntity<List<CategoryDTO>> resp = controller.getCategoriesAndStageCountByCategory(1);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody().get(0).getCategoryName()).isEqualTo("Java Development");
        assertThat(resp.getBody().get(0).getStageCount()).isEqualTo(3);
    }

    
    // ------------------------------------------------------------
    // 3) POST /assignReviewerToStage
    // ------------------------------------------------------------
    
    @Test
    @DisplayName("POST Assign Reviewer - Success")
    void assignReviewerToStage_ok() {
        ReviewerAssignmentDTO request = new ReviewerAssignmentDTO(101, 501, 1);
        when(service.assignReviewerToStage(101, 501, 1)).thenReturn(true);

        ResponseEntity<String> resp = controller.assignReviewerToStage(request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo("Reviewer assignment created successfully.");
    }

    
    @Test
    @DisplayName("POST Assign Reviewer - Category Not Found (Fail)")
    void assignReviewerToStage_categoryNotFound() {
        ReviewerAssignmentDTO request = new ReviewerAssignmentDTO(101, 999, 1);
        when(service.assignReviewerToStage(anyInt(), anyInt(), anyInt()))
                .thenThrow(new CategoryNotFound("Category with ID 999 not found"));

        try {
            controller.assignReviewerToStage(request);
            org.junit.jupiter.api.Assertions.fail("Expected CategoryNotFound");
        } catch (CategoryNotFound ex) {
            assertThat(ex.getMessage()).contains("999");
        }
    }

    
    @Test
    @DisplayName("POST Assign Reviewer - Reviewer Already Assigned (Fail)")
    void assignReviewerToStage_alreadyAssigned() {
        ReviewerAssignmentDTO request = new ReviewerAssignmentDTO(101, 501, 1);
        when(service.assignReviewerToStage(anyInt(), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("Reviewer is already assigned to a stage."));

        try {
            controller.assignReviewerToStage(request);
            org.junit.jupiter.api.Assertions.fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Reviewer is already assigned to a stage.");
        }
    }

    
    // ------------------------------------------------------------
    // 4) GET /assignedReviewerDetails
    // ------------------------------------------------------------
    
    @Test
    @DisplayName("GET Assigned Reviewer Details - Success")
    void assignedReviewerDetails_ok() {
        AssignedReviewerDTO dto = new AssignedReviewerDTO(101, "Reviewer One", 501, "Java", 1);
        when(service.assignedReviewerDetails()).thenReturn(List.of(dto));

        ResponseEntity<List<AssignedReviewerDTO>> resp = controller.assignedReviewerDetails();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
        assertThat(resp.getBody().get(0).getName()).isEqualTo("Reviewer One");
    }

    
    // ------------------------------------------------------------
    // 5) DELETE /removeReviewerFromStage
    // ------------------------------------------------------------
    
    @Test
    @DisplayName("DELETE Remove Reviewer - Success")
    void removeReviewerFromStage_ok() {
        when(service.removeReviewerFromStage(101, 501, 1)).thenReturn(true);

        ResponseEntity<String> resp = controller.removeReviewerFromStage(101, 501, 1);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("Successfully removed");
    }

    
    @Test
    @DisplayName("DELETE Remove Reviewer - Assignment Not Found (Fail)")
    void removeReviewerFromStage_notFound() {
        when(service.removeReviewerFromStage(anyInt(), anyInt(), anyInt()))
                .thenThrow(new ResourceNotFoundException("No active assignment found"));

        try {
            controller.removeReviewerFromStage(101, 501, 1);
            org.junit.jupiter.api.Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException ex) {
            assertThat(ex.getMessage()).isEqualTo("No active assignment found");
        }
    }
}