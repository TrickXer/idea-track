package com.ideatrack.main.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.ReviewerCategory;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.reviewerAssignment.AssignedReviewerDTO;
import com.ideatrack.main.dto.reviewerAssignment.AvailableReviewersDTO;
import com.ideatrack.main.dto.reviewerAssignment.CategoryDTO;
import com.ideatrack.main.exception.CategoryNotFound;
import com.ideatrack.main.exception.ResourceNotFoundException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.repository.ICategoryRepository;
import com.ideatrack.main.repository.IReviewerStageAssignmentRepository;
import com.ideatrack.main.repository.IUserRepository;

@ExtendWith(MockitoExtension.class)
class TestReviewerStageAssignmentService {

    @Mock private IReviewerStageAssignmentRepository reviewerStageRepo;
    @Mock private ICategoryRepository categoryRepo;
    @Mock private IUserRepository userRepo;

    @InjectMocks
    private ReviewerStageAssignmentService service;

    private ModelMapper modelMapper;
    private User reviewer;
    private Category category;

    @BeforeEach
    void setup() {
        // Initialize Real ModelMapper and inject it into the service
        modelMapper = new ModelMapper();
        ReflectionTestUtils.setField(service, "modelMapper", modelMapper);

        // Setup common test data
        reviewer = new User();
        reviewer.setUserId(101);
        reviewer.setName("Reviewer Alpha");
        reviewer.setRole(Constants.Role.REVIEWER);

        category = new Category();
        category.setCategoryId(501);
        category.setName("AI Research");
        category.setStageCount(3);
    }

    // ------------------------------------------------------------
    // 1) getAvailableReviewersList
    // ------------------------------------------------------------
    @Test
    @DisplayName("getAvailableReviewersList - Should filter out already assigned reviewers")
    void getAvailableReviewersList_ok() {
        User unassignedReviewer = new User();
        unassignedReviewer.setUserId(102);
        unassignedReviewer.setName("Reviewer Beta");

        ReviewerCategory activeAssignment = new ReviewerCategory();
        activeAssignment.setReviewer(reviewer); // ID 101 is already assigned

        when(userRepo.findByRoleAndDepartment_DeptIdAndDeletedFalse(Constants.Role.REVIEWER, 1))
            .thenReturn(List.of(reviewer, unassignedReviewer));
        when(reviewerStageRepo.findByDeletedFalse())
            .thenReturn(List.of(activeAssignment));

        List<AvailableReviewersDTO> result = service.getAvailableReviewersList(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Reviewer Beta");
    }

    // ------------------------------------------------------------
    // 2) getCategoriesAndStageCountByCategory
    // ------------------------------------------------------------
    @Test
    @DisplayName("getCategoriesAndStageCountByCategory - Should map Category to CategoryDTO")
    void getCategoriesAndStageCountByCategory_ok() {
        when(categoryRepo.findByDepartment_DeptIdAndDeletedFalse(1)).thenReturn(List.of(category));

        List<CategoryDTO> result = service.getCategoriesAndStageCountByCategory(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryName()).isEqualTo("AI Research");
        assertThat(result.get(0).getStageCount()).isEqualTo(3);
    }

    // ------------------------------------------------------------
    // 3) assignReviewerToStage
    // ------------------------------------------------------------
    @Test
    @DisplayName("assignReviewerToStage - Success Path")
    void assignReviewerToStage_success() {
        when(categoryRepo.findById(501)).thenReturn(Optional.of(category));
        when(userRepo.findById(101)).thenReturn(Optional.of(reviewer));
        when(reviewerStageRepo.existsByReviewer_UserIdAndDeletedFalse(101)).thenReturn(false);

        boolean status = service.assignReviewerToStage(101, 501, 2);

        assertThat(status).isTrue();
        verify(reviewerStageRepo).save(any(ReviewerCategory.class));
    }

    @Test
    @DisplayName("assignReviewerToStage - Throws UserNotFound")
    void assignReviewerToStage_userNotFound() {
        when(categoryRepo.findById(501)).thenReturn(Optional.of(category));
        when(userRepo.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignReviewerToStage(999, 501, 1))
            .isInstanceOf(UserNotFoundException.class);
    }
    
    
    @Test
    @DisplayName("assignReviewerToStage - Throws CategoryNotFound when ID is invalid")
    void assignReviewerToStage_categoryNotFound() {
        when(categoryRepo.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignReviewerToStage(101, 999, 1))
            .isInstanceOf(CategoryNotFound.class)
            .hasMessageContaining("Category with ID 999 not found");
            
        verify(reviewerStageRepo, never()).save(any(ReviewerCategory.class));
    }
    

    @Test
    @DisplayName("assignReviewerToStage - Throws IllegalStateException when double assigned")
    void assignReviewerToStage_alreadyAssigned() {
        when(categoryRepo.findById(501)).thenReturn(Optional.of(category));
        when(userRepo.findById(101)).thenReturn(Optional.of(reviewer));
        when(reviewerStageRepo.existsByReviewer_UserIdAndDeletedFalse(101)).thenReturn(true);

        assertThatThrownBy(() -> service.assignReviewerToStage(101, 501, 1))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already assigned");
    }

    @Test
    @DisplayName("assignReviewerToStage - Throws IllegalArgumentException for invalid stage")
    void assignReviewerToStage_invalidStage() {
        when(categoryRepo.findById(501)).thenReturn(Optional.of(category));
        when(userRepo.findById(101)).thenReturn(Optional.of(reviewer));

        assertThatThrownBy(() -> service.assignReviewerToStage(101, 501, 5))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("only has 3 stages");
    }

    // ------------------------------------------------------------
    // 4) assignedReviewerDetails
    // ------------------------------------------------------------
    @Test
    @DisplayName("assignedReviewerDetails - Should map ReviewerCategory to AssignedReviewerDTO")
    void assignedReviewerDetails_ok() {
        ReviewerCategory assignment = new ReviewerCategory();
        assignment.setReviewer(reviewer);
        assignment.setCategory(category);
        assignment.setAssignedStageId(1);

        when(reviewerStageRepo.findByDeletedFalse()).thenReturn(List.of(assignment));

        List<AssignedReviewerDTO> result = service.assignedReviewerDetails();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Reviewer Alpha");
        assertThat(result.get(0).getCategoryName()).isEqualTo("AI Research");
    }

    // ------------------------------------------------------------
    // 5) removeReviewerFromStage
    // ------------------------------------------------------------
    @Test
    @DisplayName("removeReviewerFromStage - Should set deleted=true and save")
    void removeReviewerFromStage_success() {
        ReviewerCategory assignment = new ReviewerCategory();
        assignment.setDeleted(false);

        when(reviewerStageRepo.findByReviewer_UserIdAndCategory_CategoryIdAndAssignedStageIdAndDeletedFalse(101, 501, 1))
            .thenReturn(Optional.of(assignment));

        boolean status = service.removeReviewerFromStage(101, 501, 1);

        assertThat(status).isTrue();
        assertThat(assignment.isDeleted()).isTrue();
        verify(reviewerStageRepo).save(assignment);
    }

    @Test
    @DisplayName("removeReviewerFromStage - Throws ResourceNotFoundException")
    void removeReviewerFromStage_fail() {
        when(reviewerStageRepo.findByReviewer_UserIdAndCategory_CategoryIdAndAssignedStageIdAndDeletedFalse(anyInt(), anyInt(), anyInt()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeReviewerFromStage(101, 501, 1))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}