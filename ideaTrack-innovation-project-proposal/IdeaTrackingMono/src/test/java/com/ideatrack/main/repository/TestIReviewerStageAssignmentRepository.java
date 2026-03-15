package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.ReviewerCategory;
import com.ideatrack.main.data.User;

@DataJpaTest
//@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestIReviewerStageAssignmentRepository {

    @Autowired
    private IReviewerStageAssignmentRepository reviewerStageRepo;

    @Autowired
    private TestEntityManager entityManager;

    private User sharedReviewer;
    private Category sharedCategory;

    @BeforeEach
    void setup() {
        sharedReviewer = User.builder().name("Default Reviewer").email("default.reviewer@test.com").role(Constants.Role.REVIEWER).deleted(false).build();
        sharedReviewer = entityManager.persistFlushFind(sharedReviewer);
        sharedCategory = Category.builder().name("General Technology").stageCount(3).deleted(false).build();
        sharedCategory = entityManager.persistFlushFind(sharedCategory);
    }
    

    
//  Tested by - Advait

    @Test
    @DisplayName("Find all active assignments - ignores soft-deleted records")
    void testFindByDeletedFalse() {
        User rev1 = entityManager.persistFlushFind(User.builder().name("Rev 1").email("rev1@test.com").role(Constants.Role.REVIEWER).deleted(false).build());
        User rev2 = entityManager.persistFlushFind(User.builder().name("Rev 2").email("rev2@test.com").role(Constants.Role.REVIEWER).deleted(false).build());
        Category cat = entityManager.persistFlushFind(Category.builder().name("General").deleted(false).build());

        ReviewerCategory active = new ReviewerCategory();
        active.setReviewer(rev1);
        active.setCategory(cat);
        active.setAssignedStageId(1);
        active.setDeleted(false);

        ReviewerCategory deleted = new ReviewerCategory();
        deleted.setReviewer(rev2);
        deleted.setCategory(cat);
        deleted.setAssignedStageId(2);
        deleted.setDeleted(true);

        entityManager.persist(active);
        entityManager.persist(deleted);
        entityManager.flush();

        List<ReviewerCategory> activeAssignments = reviewerStageRepo.findByDeletedFalse();

        assertTrue(activeAssignments.stream().anyMatch(a -> a.getReviewer().getName().equals("Rev 1")));
        assertTrue(activeAssignments.stream().noneMatch(a -> a.isDeleted()));
    }
    
    

//  Tested by - Advait

    @Test
    @DisplayName("Check if Reviewer is already assigned to any stage")
    void testExistsByReviewer_UserIdAndDeletedFalse() {
        ReviewerCategory assignment = new ReviewerCategory();
        assignment.setReviewer(sharedReviewer);
        assignment.setCategory(sharedCategory);
        assignment.setAssignedStageId(1);
        assignment.setDeleted(false);
        entityManager.persistFlushFind(assignment);

        boolean exists = reviewerStageRepo.existsByReviewer_UserIdAndDeletedFalse(sharedReviewer.getUserId());
        boolean nonExistent = reviewerStageRepo.existsByReviewer_UserIdAndDeletedFalse(999);

        assertTrue(exists, "Should return true for assigned reviewer");
        assertFalse(nonExistent, "Should return false for unassigned ID");
    }

    
    
//  Tested by - Advait
    
    @Test
    @DisplayName("Find specific assignment for removal/deletion")
    void testFindSpecificAssignment() {
        ReviewerCategory assignment = new ReviewerCategory();
        assignment.setReviewer(sharedReviewer);
        assignment.setCategory(sharedCategory);
        assignment.setAssignedStageId(2);
        assignment.setDeleted(false);
        entityManager.persistFlushFind(assignment);

        Optional<ReviewerCategory> found = reviewerStageRepo.findByReviewer_UserIdAndCategory_CategoryIdAndAssignedStageIdAndDeletedFalse(
            sharedReviewer.getUserId(), sharedCategory.getCategoryId(), 2);

        assertTrue(found.isPresent(), "Should find the specific assignment");
        assertEquals(sharedReviewer.getUserId(), found.get().getReviewer().getUserId());
        assertEquals(2, found.get().getAssignedStageId());
    }
}