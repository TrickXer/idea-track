package com.ideatrack.main.repository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// Keeping your existing annotation style consistent with other tests you shared:
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.data.AssignedReviewerToIdea;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestIAssignedReviewerToIdeaRepository {

    @Autowired
    private IAssignedReviewerToIdeaRepository assignedRepo;

    @Autowired
    private TestEntityManager entityManager;

    private Department dept;
    private User owner;
    private User reviewerA;
    private User reviewerB;
    private Idea idea1;
    private Idea idea2;

    @BeforeEach
    void setup() {
        // Department
        dept = new Department();
        dept.setDeptId(10);
        dept.setDeptName("Operations");
        dept = entityManager.persistFlushFind(dept);

        // Owner (idea submitter)
        owner = User.builder()
                .name("Owner One")
                .email("owner1@ideatrack.com")
                .password("test@123")
                .role(Constants.Role.EMPLOYEE)
                .department(dept)
                .deleted(false)
                .build();
        owner = entityManager.persistFlushFind(owner);

        // Reviewers
        reviewerA = User.builder()
                .name("Reviewer A")
                .email("rev.a@ideatrack.com")
                .password("test@123")
                .role(Constants.Role.REVIEWER)
                .department(dept)
                .deleted(false)
                .build();
        reviewerA = entityManager.persistFlushFind(reviewerA);

        reviewerB = User.builder()
                .name("Reviewer B")
                .email("rev.b@ideatrack.com")
                .password("test@123")
                .role(Constants.Role.REVIEWER)
                .department(dept)
                .deleted(false)
                .build();
        reviewerB = entityManager.persistFlushFind(reviewerB);

        // Ideas
        idea1 = new Idea();
        idea1.setTitle("Process Mining");
        idea1.setUser(owner);
        idea1.setStage(1);
        idea1.setIdeaStatus(Constants.IdeaStatus.SUBMITTED);
        idea1.setDeleted(false);
        idea1 = entityManager.persistFlushFind(idea1);

        idea2 = new Idea();
        idea2.setTitle("Another Idea");
        idea2.setUser(owner);
        idea2.setStage(1);
        idea2.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        idea2.setDeleted(false);
        idea2 = entityManager.persistFlushFind(idea2);
    }

    @Test
    @DisplayName("findByIdea_IdeaIdOrderByStageAsc → returns reviewer rows sorted by stage (asc)")
    void testFindByIdea_IdeaIdOrderByStageAsc_sortedByStage() {
        // Arrange: create two assignments for idea1 with different stages
        var rowStage2 = assign(reviewerA, idea1, 2, "Feedback stage 2");
        var rowStage1 = assign(reviewerB, idea1, 1, "Feedback stage 1");
        entityManager.persistAndFlush(rowStage2);
        entityManager.persistAndFlush(rowStage1);

        // Act
        List<AssignedReviewerToIdea> rows =
                assignedRepo.findByIdea_IdeaIdOrderByStageAsc(idea1.getIdeaId());

        // Assert: stage ascending -> 1 first, then 2
        assertEquals(2, rows.size(), "Should return both assignments for the idea");
        assertEquals(1, rows.get(0).getStage());
        assertEquals(2, rows.get(1).getStage());
        // Sanity: reviewers come through
        assertEquals("Reviewer B", rows.get(0).getReviewer().getName());
        assertEquals("Reviewer A", rows.get(1).getReviewer().getName());
    }

    @Test
    @DisplayName("findByIdea_IdeaIdOrderByStageAsc → filters by ideaId (excludes other ideas)")
    void testFindByIdea_IdeaIdOrderByStageAsc_filtersByIdea() {
        // Arrange: one assignment for idea1 and one for idea2
        var r1 = assign(reviewerA, idea1, 1, "Idea1 stage1");
        var r2 = assign(reviewerB, idea2, 1, "Idea2 stage1");
        entityManager.persistAndFlush(r1);
        entityManager.persistAndFlush(r2);

        // Act: query only for idea1
        List<AssignedReviewerToIdea> rowsIdea1 =
                assignedRepo.findByIdea_IdeaIdOrderByStageAsc(idea1.getIdeaId());

        // Assert
        assertEquals(1, rowsIdea1.size());
        assertEquals(idea1.getIdeaId(), rowsIdea1.get(0).getIdea().getIdeaId());
        assertEquals("Reviewer A", rowsIdea1.get(0).getReviewer().getName());
    }

    @Test
    @DisplayName("findByIdea_IdeaIdOrderByStageAsc → empty when no assignments exist")
    void testFindByIdea_IdeaIdOrderByStageAsc_empty() {
        // Act
        List<AssignedReviewerToIdea> rows =
                assignedRepo.findByIdea_IdeaIdOrderByStageAsc(idea1.getIdeaId());

        // Assert
        assertTrue(rows.isEmpty(), "No assignments yet -> empty list");
    }

    // -------------------
    // Helper to create a row
    // -------------------
    private AssignedReviewerToIdea assign(User reviewer, Idea idea, int stage, String feedback) {
        AssignedReviewerToIdea row = new AssignedReviewerToIdea();
        row.setReviewer(reviewer);
        row.setIdea(idea);
        row.setStage(stage);
        row.setFeedback(feedback);
        // set other defaults if your entity requires (e.g., deleted=false)
        return row;
    }
 // ==========================================================
 // ReviewerModule
 // ==========================================================

 @Test
 @DisplayName("ReviewerModule: findDashboardIdeas → returns non-deleted assignments for reviewer (decision=null means no decision filter)")
 void reviewerModule_findDashboardIdeas_noDecisionFilter() {

     // Arrange
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb1", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea2, 1, "fb2", "REJECTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea2, 2, "fb3", "ACCEPTED", true)); // deleted -> excluded
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerB, idea1, 1, "fb4", "ACCEPTED", false)); // other reviewer -> excluded

     // Act
     List<AssignedReviewerToIdea> rows = assignedRepo.findDashboardIdeas(reviewerA.getUserId(), null);

     // Assert
     assertEquals(2, rows.size());
     assertTrue(rows.stream().allMatch(r -> r.getReviewer().getUserId().equals(reviewerA.getUserId())));
     assertTrue(rows.stream().allMatch(r -> !r.isDeleted()));   // ✅ correct getter: isDeleted()
 }

 @Test
 @DisplayName("ReviewerModule: findDashboardIdeas → filters by decision and excludes deleted")
 void reviewerModule_findDashboardIdeas_withDecisionFilter() {

     // Arrange
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb1", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea2, 1, "fb2", "REJECTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea2, 2, "fb3", "ACCEPTED", true)); // deleted

     // Act
     List<AssignedReviewerToIdea> rows = assignedRepo.findDashboardIdeas(reviewerA.getUserId(), "ACCEPTED");

     // Assert
     assertEquals(1, rows.size());
     assertEquals("ACCEPTED", rows.get(0).getDecision());
     assertTrue(!rows.get(0).isDeleted());
 }

 @Test
 @DisplayName("ReviewerModule: findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse → returns only active row")
 void reviewerModule_strictLookup_deletedFalse() {

     // Arrange
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 2, "fb", "REJECTED", true)); // deleted

     // Act + Assert
     assertTrue(
         assignedRepo.findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
             idea1.getIdeaId(), reviewerA.getUserId(), 1
         ).isPresent()
     );

     assertTrue(
         assignedRepo.findByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
             idea1.getIdeaId(), reviewerA.getUserId(), 2
         ).isEmpty()
     );
 }

 @Test
 @DisplayName("ReviewerModule: findByIdea_IdeaIdAndStageAndDeletedFalse → returns only stage rows, excludes deleted")
 void reviewerModule_findByIdeaAndStage_deletedFalse() {

     // Arrange
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerB, idea1, 1, "fb", "ACCEPTED", true));  // deleted
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerB, idea1, 2, "fb", "REJECTED", false));

     // Act
     List<AssignedReviewerToIdea> stage1 =
         assignedRepo.findByIdea_IdeaIdAndStageAndDeletedFalse(idea1.getIdeaId(), 1);

     // Assert
     assertEquals(1, stage1.size());
     assertEquals(1, stage1.get(0).getStage());
     assertEquals(reviewerA.getUserId(), stage1.get(0).getReviewer().getUserId());
     assertTrue(!stage1.get(0).isDeleted());
 }

 @Test
 @DisplayName("ReviewerModule: findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc → sorted by stage asc, excludes deleted")
 void reviewerModule_findByIdeaDeletedFalse_orderByStageAsc() {

     // Arrange (out-of-order stages + a deleted row)
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 3, "fb", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb", "REJECTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerB, idea1, 2, "fb", "REFINE", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerB, idea1, 4, "fb", "ACCEPTED", true)); // deleted

     // Act
     List<AssignedReviewerToIdea> rows =
         assignedRepo.findByIdea_IdeaIdAndDeletedFalseOrderByStageAsc(idea1.getIdeaId());

     // Assert
     assertEquals(3, rows.size());
     assertEquals(1, rows.get(0).getStage());
     assertEquals(2, rows.get(1).getStage());
     assertEquals(3, rows.get(2).getStage());
     assertTrue(rows.stream().allMatch(r -> !r.isDeleted()));
 }

 @Test
 @DisplayName("ReviewerModule: findDashboardIdeasPaged → paginates + decision filter uses UPPER(a.decision)")
 void reviewerModule_findDashboardIdeasPaged() {

     // Arrange (3 accepted, 1 rejected, 1 deleted accepted)
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb", "accepted", false)); // lowercase to prove UPPER compare works
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 2, "fb", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea2, 1, "fb", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea2, 2, "fb", "REJECTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea2, 3, "fb", "ACCEPTED", true)); // deleted excluded

     // Act (must pass decision param as UPPER because query compares UPPER(a.decision) = :decision)
     var page0 = assignedRepo.findDashboardIdeasPaged(
         reviewerA.getUserId(),
         "ACCEPTED",
         PageRequest.of(0, 2)
     );

     var page1 = assignedRepo.findDashboardIdeasPaged(
         reviewerA.getUserId(),
         "ACCEPTED",
         PageRequest.of(1, 2)
     );

     // Assert (3 accepted total, deleted excluded)
     assertEquals(2, page0.getContent().size());
     assertEquals(1, page1.getContent().size());
     assertEquals(3, page0.getTotalElements());

     assertTrue(page0.getContent().stream().allMatch(r -> "ACCEPTED".equalsIgnoreCase(r.getDecision())));
     assertTrue(page0.getContent().stream().allMatch(r -> !r.isDeleted()));
 }

 @Test
 @DisplayName("ReviewerModule: findByIdea_IdeaIdAndDeletedFalseAndDecisionIsNotNull → returns only decided & non-deleted rows")
 void reviewerModule_decisionHistoryPaged() {

     // Arrange: one decided, one undecided, one decided but deleted
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 2, "fb", null, false));     // decision null -> excluded
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerB, idea1, 3, "fb", "REJECTED", true)); // deleted -> excluded

     // Act
     var page = assignedRepo.findByIdea_IdeaIdAndDeletedFalseAndDecisionIsNotNull(
         idea1.getIdeaId(),
         PageRequest.of(0, 10)
     );

     // Assert
     assertEquals(1, page.getTotalElements());
     assertEquals("ACCEPTED", page.getContent().get(0).getDecision());
     assertTrue(!page.getContent().get(0).isDeleted());
 }

 @Test
 @DisplayName("ReviewerModule: existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse → true only for non-deleted row")
 void reviewerModule_exists_deletedFalse() {

     // Arrange
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 1, "fb", "ACCEPTED", false));
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 2, "fb", "REJECTED", true));

     // Act
     boolean existsStage1 = assignedRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
         idea1.getIdeaId(), reviewerA.getUserId(), 1);

     boolean existsStage2 = assignedRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStageAndDeletedFalse(
         idea1.getIdeaId(), reviewerA.getUserId(), 2);

     // Assert
     assertTrue(existsStage1);
     assertTrue(!existsStage2);
 }

 @Test
 @DisplayName("ReviewerModule: existsByIdea_IdeaIdAndReviewer_UserIdAndStage → true even if only deleted exists (no deleted filter)")
 void reviewerModule_exists_withoutDeletedFilter() {

     // Arrange: only deleted row
     entityManager.persistAndFlush(assignWithDecisionAndDeleted(reviewerA, idea1, 5, "fb", "ACCEPTED", true));

     // Act
     boolean exists = assignedRepo.existsByIdea_IdeaIdAndReviewer_UserIdAndStage(
         idea1.getIdeaId(), reviewerA.getUserId(), 5);

     // Assert
     assertTrue(exists);
 }

 // -------------------
 // ReviewerModule helper (matches your entity fields exactly)
 // -------------------
 private AssignedReviewerToIdea assignWithDecisionAndDeleted(
         User reviewer, Idea idea, int stage, String feedback, String decision, boolean deleted) {

     AssignedReviewerToIdea row = new AssignedReviewerToIdea();
     row.setReviewer(reviewer);
     row.setIdea(idea);
     row.setStage(stage);
     row.setFeedback(feedback);
     row.setDecision(decision);
     row.setDeleted(deleted);

     // createdAt / updatedAt are NOT NULL in entity.
     // If auditing isn't populating in your @DataJpaTest, this prevents null constraint failures.
     // If auditing DOES populate, setting them manually is still safe for tests.
     LocalDateTime now = LocalDateTime.now();
     row.setCreatedAt(now.minusDays(1));
     row.setUpdatedAt(now);

     return row;
 }
}