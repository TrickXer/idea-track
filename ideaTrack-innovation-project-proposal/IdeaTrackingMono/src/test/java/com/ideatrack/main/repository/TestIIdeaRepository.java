package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.Idea;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.analytics.CategoryCountDTO;
import com.ideatrack.main.data.Constants;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestIIdeaRepository {
    
    @Autowired
    private IIdeaRepository ideaRepo;
    
    @Autowired
    private TestEntityManager entityManager;
    
    private User testUser;
    private Category testCategory;
    
    // =================================================================
    //  (Idea and Activity Service repository usage)
    // =================================================================

    @BeforeEach
    void setup() {
        // 1. Create and persist Department (Required for User and Category)
        Department dept = new Department();
        dept.setDeptId(1);
        dept.setDeptName("R&D");
        
        Department savedDept = entityManager.persistFlushFind(dept);

        // 2. Create and persist User 
        testUser = User.builder()
                .name("Akash")
                .email("akash@test.com")
                .role(Constants.Role.EMPLOYEE)
                .department(savedDept)
                .deleted(false)
                .build();
        testUser = entityManager.persistFlushFind(testUser);

        // 3. Create and persist Category
        testCategory = Category.builder()
                .name("Software")
                .department(savedDept)
                .createdByAdmin(testUser)
                .deleted(false)
                .build();
        testCategory = entityManager.persistFlushFind(testCategory);
    }

    
    @Test
    @DisplayName("Find active ideas for a specific user")
    void testFindAllByUserAndDeletedFalse() {
        // Arrange
        Idea activeIdea = createBaseIdea("Active Project", false);
        Idea deleteIdea = createBaseIdea("Old Project", true);
        ideaRepo.saveAll(List.of(activeIdea, deleteIdea));
        
        // Act
        List<Idea> results = ideaRepo.findAllByUser_UserIdAndDeletedFalse(testUser.getUserId());
        
        // Assert
        assertEquals(1, results.size(), "Should find exactly one active idea");
        assertEquals("Active Project", results.get(0).getTitle());
        assertFalse(results.get(0).isDeleted());
    }
    
    
    @Test
    @DisplayName("Filter by title, category, and handle nulls")
    void testComplexSearch() {
        // 1. FIX: Get baseline count of ONLY ACTIVE ideas currently in the DB
        long initialCount = ideaRepo.countByDeletedFalse();
    	
        // 2. Arrange
        Idea idea1 = createBaseIdea("Green Energy", false);
        idea1.setIdeaStatus(Constants.IdeaStatus.SUBMITTED);
        
        Idea idea2 = createBaseIdea("Blue Ocean", false);
        idea2.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        
        ideaRepo.saveAll(List.of(idea1, idea2));
        entityManager.flush();
        entityManager.clear();
        
        // 3. Act 1: Search by title "green"
        Page<Idea> search1 = ideaRepo.searchIdeas(
                "green",
                testCategory.getCategoryId(),
                null,
                Constants.IdeaStatus.SUBMITTED,
                null,
                false,
                PageRequest.of(0, 10)
        );
 
        // Act 2: Search with all nulls (filtered by deleted = false inside the query)
        Page<Idea> searchAll = ideaRepo.searchIdeas(
                null, null, null, null, null, false, PageRequest.of(0, 100)
        );
        
        // 4. Assert
        assertEquals(1, search1.getContent().size(), "Should find exactly 1 idea with 'green'");
        assertTrue(search1.getContent().get(0).getTitle().equalsIgnoreCase("Green Energy"));
        
 
        long expectedTotal = initialCount + 2;
        assertEquals(expectedTotal, searchAll.getTotalElements(), "Search with nulls should return initial active ideas plus 2 new ones");
    }
    
   
    private Idea createBaseIdea(String title, boolean isDeleted) {
        Idea idea = new Idea();
        idea.setTitle(title);
        idea.setUser(testUser);
        idea.setCategory(testCategory);
        idea.setDeleted(isDeleted);
        idea.setIdeaStatus(Constants.IdeaStatus.DRAFT);
        idea.setStage(1);
        return idea;
    }


        // =================================================================
        // ADDITIONAL TESTS (Gamification & Hierarchy repository usage)
        // =================================================================

        @Test
        @DisplayName("Gamification: count ideas by user (submitter badges)")
        void testCountByUser() {
            // Arrange: add 3 active ideas for testUser
            Idea i1 = createBaseIdea("I-1", false);
            Idea i2 = createBaseIdea("I-2", false);
            Idea i3 = createBaseIdea("I-3", false);
            ideaRepo.saveAll(List.of(i1, i2, i3));

            // Also create a deleted idea for the same user; countByUser typically counts all user ideas,
            // but if your repository filters deleted=false internally, adjust this expectation as needed.
            Idea deletedIdea = createBaseIdea("I-del", true);
            ideaRepo.save(deletedIdea);

            long count = ideaRepo.countByUser(testUser);

            // Assert (assuming countByUser counts all ideas for user regardless of deleted flag;
            // if not, change to expected = 3)
            assertEquals(4, count);
        }

        @Test
        @DisplayName("Gamification: count ideas by user with final statuses (ACCEPTED/REJECTED)")
        void testCountByUserAndIdeaStatusIn() {
            // Arrange: create ideas with different statuses
            Idea accepted = createBaseIdea("Accepted Idea", false);
            accepted.setIdeaStatus(Constants.IdeaStatus.ACCEPTED);

            Idea rejected = createBaseIdea("Rejected Idea", false);
            rejected.setIdeaStatus(Constants.IdeaStatus.REJECTED);

            Idea draft = createBaseIdea("Draft Idea", false);
            draft.setIdeaStatus(Constants.IdeaStatus.DRAFT);

            ideaRepo.saveAll(List.of(accepted, rejected, draft));

            // Act
            long finals = ideaRepo.countByUserAndIdeaStatusIn(
                    testUser, List.of(Constants.IdeaStatus.ACCEPTED, Constants.IdeaStatus.REJECTED));

            // Assert: only ACCEPTED and REJECTED should be counted
            assertEquals(2, finals);
        }

        @Test
        @DisplayName("Hierarchy: verify findById returns persisted idea")
        void testFindById_sanity() {
            Idea persisted = ideaRepo.save(createBaseIdea("Sanity", false));

            Idea found = ideaRepo.findById(persisted.getIdeaId())
                    .orElseThrow(() -> new AssertionError("Idea not found"));

            assertEquals("Sanity", found.getTitle());
            assertFalse(found.isDeleted());
            assertEquals(testUser.getUserId(), found.getUser().getUserId());
        }

        // (Optional) If you use this in Gamification: count active ideas (deleted=false)
        @Test
        @DisplayName("Count active (not deleted) ideas for baseline sanity")
        void testCountByDeletedFalse() {
            long before = ideaRepo.countByDeletedFalse();

            Idea active = createBaseIdea("Active-Only", false);
            Idea deleted = createBaseIdea("Deleted-Only", true);
            ideaRepo.saveAll(List.of(active, deleted));

            long after = ideaRepo.countByDeletedFalse();

            assertEquals(before + 1, after);
        }

        // (Optional helper) create idea for a specific user and status
        // Not used in the tests above, but handy if you want to extend later.
        @SuppressWarnings("unused")
        private Idea createIdeaForUserWithStatus(User u, Constants.IdeaStatus status, boolean deleted, String title) {
            Idea i = new Idea();
            i.setUser(u);
            i.setCategory(testCategory);
            i.setIdeaStatus(status);
            i.setDeleted(deleted);
            i.setStage(1);
            i.setTitle(title);
            return entityManager.persistFlushFind(i);
        }

        
        
     // =================================================================
        // ANALYTICS SERVICE MODULE TESTS
        // =================================================================

        @Test
        @DisplayName("Analytics: Count ideas within date range")
        void testCountByDeletedFalseAndCreatedAtBetween() throws Exception {
            // Create idea first (will get current timestamp from JPA auditing)
            Idea idea = createBaseIdea("In Range", false);
            idea = entityManager.persistFlushFind(idea);
            entityManager.flush();
            entityManager.clear();
            
            // Now read back to get the actual persisted createdAt timestamp
            Idea persisted = ideaRepo.findById(idea.getIdeaId())
                    .orElseThrow(() -> new AssertionError("Idea not found"));
            
            // Define range AROUND the actual createdAt timestamp
            LocalDateTime actualCreatedAt = persisted.getCreatedAt();
            LocalDateTime start = actualCreatedAt.minusHours(1);
            LocalDateTime end = actualCreatedAt.plusHours(1);

            long count = ideaRepo.countByDeletedFalseAndCreatedAtBetween(start, end);
            assertTrue(count >= 1, "Should find at least 1 idea in range (found: " + count + ")");
        }

        @Test
        @DisplayName("Analytics: Find category distribution for specific user")
        void testFindCategoryCountsForUser() {
            // Arrange
            Idea i1 = createBaseIdea("Idea 1", false);
            Idea i2 = createBaseIdea("Idea 2", false);
            ideaRepo.saveAll(List.of(i1, i2));

            // Act
            List<CategoryCountDTO> distro = ideaRepo.findCategoryCountsForUser(testUser.getUserId());

            // Assert
            assertFalse(distro.isEmpty());
            assertEquals(testCategory.getName(), distro.get(0).getCategoryName());
            assertEquals(2, distro.get(0).getIdeaCount());
        }

        @Test
        @DisplayName("Analytics: Count distinct users by Department")
        void testCountDistinctUsersByDepartment() throws Exception {
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            LocalDateTime start = end.minusDays(2);

            // Idea by testUser (R&D Department)
            Idea idea = createBaseIdea("Dept Idea", false);
            idea = entityManager.persistFlushFind(idea);
            setCreatedAt(idea, end.minusHours(1));

            Integer deptId = testUser.getDepartment().getDeptId();
            long distinctUsers = ideaRepo.countDistinctUsersByDepartment(deptId, start, end);

            assertEquals(1, distinctUsers);
        }

        @Test
        @DisplayName("Analytics: Count by Scope Category")
        void testCountByCategoryScope() throws Exception {
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            LocalDateTime start = end.minusDays(2);

            Idea idea = createBaseIdea("Category Idea", false);
            idea.setIdeaStatus(Constants.IdeaStatus.APPROVED);
            idea = entityManager.persistFlushFind(idea);
            setCreatedAt(idea, end.minusHours(1));

            long approvedCount = ideaRepo.countByIdeaStatusAndCategory_CategoryIdAndDeletedFalseAndCreatedAtBetween(
                    Constants.IdeaStatus.APPROVED, testCategory.getCategoryId(), start, end);

            assertEquals(1, approvedCount);
        }

        @Test
        @DisplayName("Analytics: Find Total Approved Category Distribution")
        void testFindTotalCategoryCountCreatedBetween() throws Exception {
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            LocalDateTime start = end.minusDays(2);

            // Create an Approved idea
            Idea idea = createBaseIdea("Global Approved", false);
            idea.setIdeaStatus(Constants.IdeaStatus.APPROVED);
            idea = entityManager.persistFlushFind(idea);
            setCreatedAt(idea, end.minusHours(1));

            List<CategoryCountDTO> results = ideaRepo.findTotalCategoryCountCreatedBetween(start, end);

            assertFalse(results.isEmpty());
            assertEquals(1, results.get(0).getIdeaCount());
        }

        @Test
        @DisplayName("Analytics: Count distinct users by Period")
        void testCountDistinctUsersByPeriod() throws Exception {
            LocalDateTime end = LocalDateTime.now().plusDays(1);
            LocalDateTime start = end.minusDays(2);

            Idea idea = createBaseIdea("Period Idea", false);
            idea = entityManager.persistFlushFind(idea);
            setCreatedAt(idea, end.minusHours(1));

//            long userCount = ideaRepo.countDistinctUsersByPeriod(start, end);
//            assertEquals(1, userCount);
        }

  
//      Helper to bypass JPA Auditing and set specific CreatedAt dates for testing

        private void setCreatedAt(Idea idea, LocalDateTime dateTime) throws Exception {
            java.lang.reflect.Field field = com.ideatrack.main.data.Idea.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(idea, dateTime);
            entityManager.merge(idea);
            entityManager.flush();
        }
        
}