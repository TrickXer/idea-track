// src/test/java/com/ideatrack/main/repository/TestIDepartmentRepository.java
package com.ideatrack.main.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ideatrack.main.data.Department;

@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestIDepartmentRepository {

    @Autowired
    private IDepartmentRepository deptRepo;

    @Autowired
    private TestEntityManager entityManager;

    private Department d401, d402, dDeleted;

    @BeforeEach
    void setup() {
        // ============
        // Departments
        // ============
        // Use IDs not in your seed (seed has 101..112)
        d401 = new Department();
        d401.setDeptId(401);
        d401.setDeptName("Test-Engineering");
        d401.setDeleted(false);
        d401 = entityManager.persistFlushFind(d401);

        d402 = new Department();
        d402.setDeptId(402);
        d402.setDeptName("Test-HR");
        d402.setDeleted(false);
        d402 = entityManager.persistFlushFind(d402);

        dDeleted = new Department();
        dDeleted.setDeptId(403);
        dDeleted.setDeptName("Test-Archived");
        dDeleted.setDeleted(true);
        dDeleted = entityManager.persistFlushFind(dDeleted);

        entityManager.flush();
        entityManager.clear();
    }

    // ------------------------------------------------------------
    // 1) findByDeptName
    // ------------------------------------------------------------
    @Test
    @DisplayName("findByDeptName returns department when name exists")
    void findByDeptName_ok() {
        Department found = deptRepo.findByDeptName("Test-Engineering");
        assertNotNull(found);
        assertEquals(401, found.getDeptId());
    }

    // ------------------------------------------------------------
    // 2) existsByDeptName
    // ------------------------------------------------------------
    @Test
    @DisplayName("existsByDeptName returns true for existing deptName")
    void existsByDeptName_true() {
        assertTrue(deptRepo.existsByDeptName("Test-HR"));
    }

    @Test
    @DisplayName("existsByDeptName returns false for missing deptName")
    void existsByDeptName_false() {
        assertFalse(deptRepo.existsByDeptName("No-Such-Dept"));
    }

    // ------------------------------------------------------------
    // 3) findByDeptId
    // ------------------------------------------------------------
    @Test
    @DisplayName("findByDeptId returns Optional present for existing deptId")
    void findByDeptId_ok() {
        Optional<Department> found = deptRepo.findByDeptId(401);
        assertTrue(found.isPresent());
        assertEquals("Test-Engineering", found.get().getDeptName());
    }

    @Test
    @DisplayName("findByDeptId returns Optional empty for missing deptId")
    void findByDeptId_missing() {
        Optional<Department> found = deptRepo.findByDeptId(9999);
        assertTrue(found.isEmpty());
    }

    // ------------------------------------------------------------
    // 4) findAllByDeletedFalse
    // ------------------------------------------------------------
    @Test
    @DisplayName("findAllByDeletedFalse returns only non-deleted departments (filtering to our test deptIds)")
    void findAllByDeletedFalse_ok() {
        List<Department> active = deptRepo.findAllByDeletedFalse();

        // Global invariant: all non-deleted
        assertTrue(active.stream().allMatch(d -> !d.isDeleted()));

        // Focus only on our deptIds (401, 402, 403)
        var ourIds = java.util.Set.of(401, 402, 403);
        List<Department> ours = active.stream()
                .filter(d -> d.getDeptId() != null && ourIds.contains(d.getDeptId()))
                .toList();

        // Only 401 and 402 are active, 403 is deleted
        assertEquals(2, ours.size());
        assertTrue(ours.stream().anyMatch(d -> d.getDeptId().equals(401)));
        assertTrue(ours.stream().anyMatch(d -> d.getDeptId().equals(402)));
        assertFalse(ours.stream().anyMatch(d -> d.getDeptId().equals(403)));
    }

    // ------------------------------------------------------------
    // 5) findByDeptNameIgnoreCaseAndDeletedFalse
    // ------------------------------------------------------------
    @Test
    @DisplayName("findByDeptNameIgnoreCaseAndDeletedFalse resolves by name case-insensitive and only active")
    void findByDeptNameIgnoreCaseAndDeletedFalse_ok() {
        Optional<Department> found1 = deptRepo.findByDeptNameIgnoreCaseAndDeletedFalse("test-engineering");
        assertTrue(found1.isPresent());
        assertEquals(401, found1.get().getDeptId());

        // Deleted one should NOT be returned
        Optional<Department> foundDeleted = deptRepo.findByDeptNameIgnoreCaseAndDeletedFalse("test-archived");
        assertTrue(foundDeleted.isEmpty());
    }
}