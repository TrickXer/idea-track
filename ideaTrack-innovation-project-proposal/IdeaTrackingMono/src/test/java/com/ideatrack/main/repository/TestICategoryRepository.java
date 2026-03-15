// src/test/java/com/ideatrack/main/repository/TestICategoryRepository.java
package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// Keep the same imports style you used in TestIIdeaRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ideatrack.main.data.Category;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.User;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestICategoryRepository {

    @Autowired
    private ICategoryRepository categoryRepo;

    @Autowired
    private TestEntityManager entityManager;

    private Department dept201, dept202;
    private User admin;

    @BeforeEach
    void setup() {
        // ===========
        // Departments
        // ===========
        // 👇 Use IDs NOT present in your seed (seed has 101..112)
        dept201 = new Department();
        dept201.setDeptId(201);
        dept201.setDeptName("Test-Engineering");
        dept201 = entityManager.persistFlushFind(dept201);

        dept202 = new Department();
        dept202.setDeptId(202);
        dept202.setDeptName("Test-HR");
        dept202 = entityManager.persistFlushFind(dept202);

        // =====
        // Admin
        // =====
        // DO NOT set userId manually; let DB assign
        admin = User.builder()
                .name("Test Admin")
                .email("test.admin@example.com")
                .role(Constants.Role.ADMIN)
                .department(dept201)
                .deleted(false)
                .build();
        admin = entityManager.persistFlushFind(admin);

        // ==========
        // Categories
        // ==========
        // Use plain '&' in Java strings (not HTML-escaped).
        Category c1 = Category.builder()
                .name("Innovation & Ideas")
                .department(dept201)        // in dept 201
                .createdByAdmin(admin)
                .reviewerCountPerStage(2)
                .stageCount(3)
                .deleted(false)
                .build();
        entityManager.persist(c1);

        Category c2 = Category.builder()
                .name("Innovation & Ideas")
                .department(dept202)        // same name, different dept
                .createdByAdmin(admin)
                .reviewerCountPerStage(1)
                .stageCount(2)
                .deleted(false)
                .build();
        entityManager.persist(c2);

        Category deleted = Category.builder()
                .name("Archived-Test")
                .department(dept201)
                .createdByAdmin(admin)
                .reviewerCountPerStage(1)
                .stageCount(1)
                .deleted(true)
                .build();
        entityManager.persist(deleted);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("findByCategoryIdAndDeletedFalse returns only non-deleted")
    void findByCategoryIdAndDeletedFalse() {
        List<Category> all = categoryRepo.findAll();
        Category active = all.stream().filter(c -> !c.isDeleted()).findFirst().orElseThrow();

        Optional<Category> found = categoryRepo.findByCategoryIdAndDeletedFalse(active.getCategoryId());
        assertTrue(found.isPresent());
        assertFalse(found.get().isDeleted());

        Category del = all.stream().filter(Category::isDeleted).findFirst().orElseThrow();
        Optional<Category> notFound = categoryRepo.findByCategoryIdAndDeletedFalse(del.getCategoryId());
        assertTrue(notFound.isEmpty(), "Deleted categories should not be found");
    }

    @Test
    @DisplayName("findAllByDeletedFalse returns only active categories (filtering to our test departments)")
    void findAllByDeletedFalse() {
        List<Category> active = categoryRepo.findAllByDeletedFalse();

        // 1) All returned rows must be non-deleted (global invariant)
        assertTrue(active.stream().allMatch(c -> !c.isDeleted()));

        // 2) Focus only on the two categories created in this test (deptId 201/202)
        var ourDeptIds = java.util.Set.of(201, 202);
        List<Category> ours = active.stream()
                .filter(c -> c.getDepartment() != null && ourDeptIds.contains(c.getDepartment().getDeptId()))
                .toList();

        // We created exactly 2 non-deleted under our test-only departments
        assertEquals(2, ours.size(), "Exactly our two categories should be present for dept 201/202");

        // Optional: verify names or other fields if you want
        assertTrue(ours.stream().anyMatch(c -> c.getName().equals("Innovation & Ideas")));
    }
    @Test
    @DisplayName("existsByNameAndDepartment_DeptIdAndDeletedFalse checks per-department uniqueness")
    void existsByNameAndDepartment() {
        assertTrue(categoryRepo.existsByNameAndDepartment_DeptIdAndDeletedFalse("Innovation & Ideas", dept201.getDeptId()));
        assertTrue(categoryRepo.existsByNameAndDepartment_DeptIdAndDeletedFalse("Innovation & Ideas", dept202.getDeptId()));
        assertFalse(categoryRepo.existsByNameAndDepartment_DeptIdAndDeletedFalse("Innovation & Ideas", 999)); // non-existent dept
        assertFalse(categoryRepo.existsByNameAndDepartment_DeptIdAndDeletedFalse("Unknown", dept201.getDeptId()));
    }

    @Test
    @DisplayName("existsActiveByNameAndDeptExcludingId excludes a given id")
    void existsActiveByNameAndDeptExcludingId() {
        List<Category> active = categoryRepo.findAllByDeletedFalse();
        Category first = active.get(0); // some "Innovation & Ideas"

        boolean excludesSelf = categoryRepo.existsActiveByNameAndDeptExcludingId(
                first.getName(),
                first.getDepartment().getDeptId(),
                first.getCategoryId()
        );
        assertFalse(excludesSelf, "Excluding the same record should not create conflict");

        boolean conflict = categoryRepo.existsActiveByNameAndDeptExcludingId(
                first.getName(),
                first.getDepartment().getDeptId(),
                -1 // bogus id simulates another record
        );
        assertTrue(conflict);
    }
    
    
    
//  Testing findByDepartment_DeptIdAndDeletedFalse  - Advait.
    @Test
    @DisplayName("Find active Categories by Department ID")
    void testFindByDepartment_DeptIdAndDeletedFalse() {
        Department itDept = entityManager.<Department>persistFlushFind(new Department(1, "IT", null, null, false));
        Department hrDept = entityManager.<Department>persistFlushFind(new Department(2, "HR", null, null, false));

        Category cat1 = Category.builder().name("Java").department(itDept).deleted(false).build();
        Category cat2 = Category.builder().name("Hiring").department(hrDept).deleted(false).build();
        Category cat3 = Category.builder().name("Legacy IT").department(itDept).deleted(true).build();

        entityManager.persist(cat1);
        entityManager.persist(cat2);
        entityManager.persist(cat3);
        entityManager.flush();

        List<Category> results = categoryRepo.findByDepartment_DeptIdAndDeletedFalse(itDept.getDeptId());

        assertEquals(1, results.size(), "Should only find active IT category");
        assertEquals("Java", results.get(0).getName());
    }
}