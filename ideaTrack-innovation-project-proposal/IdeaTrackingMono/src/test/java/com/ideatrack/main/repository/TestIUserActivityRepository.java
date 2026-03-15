// src/test/java/com/ideatrack/main/repository/TestIUserRepository.java
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
 
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.User;
 
@DataJpaTest
@ExtendWith(SpringExtension.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestIUserActivityRepository {
 
    @Autowired
    private IUserRepository userRepo;
 
    @Autowired
    private TestEntityManager entityManager;
 
    private Department dept301;
    private User uActiveEmp, uActiveReviewer, uDeletedEmp, uActiveAdmin;
 
    @BeforeEach
    void setup() {
        // =============
        // Department(s)
        // =============
        // Use IDs not in your seed (seed has 101..112) -> choose 301+
        dept301 = new Department();
        dept301.setDeptId(301);
        dept301.setDeptName("Test-Dept-Users");
        dept301 = entityManager.persistFlushFind(dept301);
 
        // =====
        // Users
        // =====
        // DO NOT set userId manually if userId is auto-generated
        uActiveEmp = User.builder()
                .name("Active Emp")
                .email("active.emp@test.com")
                .department(dept301)
                .role(Constants.Role.EMPLOYEE)
                .status(Constants.Status.ACTIVE)
                .deleted(false)
                .build();
        uActiveEmp = entityManager.persistFlushFind(uActiveEmp);
 
        uActiveReviewer = User.builder()
                .name("Active Reviewer")
                .email("active.reviewer@test.com")
                .department(dept301)
                .role(Constants.Role.REVIEWER)
                .status(Constants.Status.ACTIVE)
                .deleted(false)
                .build();
        uActiveReviewer = entityManager.persistFlushFind(uActiveReviewer);
 
        uActiveAdmin = User.builder()
                .name("Active Admin")
                .email("active.admin@test.com")
                .department(dept301)
                .role(Constants.Role.ADMIN)
                .status(Constants.Status.ACTIVE)
                .deleted(false)
                .build();
        uActiveAdmin = entityManager.persistFlushFind(uActiveAdmin);
 
        uDeletedEmp = User.builder()
                .name("Deleted Emp")
                .email("deleted.emp@test.com")
                .department(dept301)
                .role(Constants.Role.EMPLOYEE)
                .status(Constants.Status.INACTIVE)
                .deleted(true)
                .build();
        uDeletedEmp = entityManager.persistFlushFind(uDeletedEmp);
 
        entityManager.flush();
        entityManager.clear();
    }
 
    // ------------------------------------------------------------
    // 1) findByEmail
    // ------------------------------------------------------------
    @Test
    @DisplayName("findByEmail returns user when email exists")
    void findByEmail_ok() {
        Optional<User> found = userRepo.findByEmail("active.emp@test.com");
        assertTrue(found.isPresent());
        assertEquals("Active Emp", found.get().getName());
        assertFalse(found.get().isDeleted());
    }
 
    @Test
    @DisplayName("findByEmail returns empty when email does not exist")
    void findByEmail_notFound() {
        Optional<User> found = userRepo.findByEmail("missing@test.com");
        assertTrue(found.isEmpty());
    }
 
    // ------------------------------------------------------------
    // 2) existsByEmail
    // ------------------------------------------------------------
    @Test
    @DisplayName("existsByEmail returns true for existing email")
    void existsByEmail_true() {
        assertTrue(userRepo.existsByEmail("active.reviewer@test.com"));
    }
 
    @Test
    @DisplayName("existsByEmail returns false for missing email")
    void existsByEmail_false() {
        assertFalse(userRepo.existsByEmail("nope@test.com"));
    }
 
    // ------------------------------------------------------------
    // 3) findAllByDeletedFalse
    // ------------------------------------------------------------
    @Test
    @DisplayName("findAllByDeletedFalse returns only non-deleted users (filtering to our test department)")
    void findAllByDeletedFalse_ok() {
        List<User> active = userRepo.findAllByDeletedFalse();
 
        // Global invariant: all are non-deleted
        assertTrue(active.stream().allMatch(u -> !u.isDeleted()));
 
        // Focus only on our test department rows (deptId 301)
        List<User> ours = active.stream()
                .filter(u -> u.getDepartment() != null && u.getDepartment().getDeptId() != null
&& u.getDepartment().getDeptId().equals(301))
                .toList();
 
        // We created 3 non-deleted users under dept301
        assertEquals(3, ours.size());
        assertTrue(ours.stream().anyMatch(u -> u.getEmail().equals("active.emp@test.com")));
        assertTrue(ours.stream().anyMatch(u -> u.getEmail().equals("active.reviewer@test.com")));
        assertTrue(ours.stream().anyMatch(u -> u.getEmail().equals("active.admin@test.com")));
    }
 
    // ------------------------------------------------------------
    // 4) findAllByDeletedFalseAndRoleIn
    // ------------------------------------------------------------
    @Test
    @DisplayName("findAllByDeletedFalseAndRoleIn returns only non-deleted users with allowed roles (filtering to our test department)")
    void findAllByDeletedFalseAndRoleIn_ok() {
        List<Constants.Role> roles = List.of(Constants.Role.REVIEWER, Constants.Role.EMPLOYEE);
 
        List<User> result = userRepo.findAllByDeletedFalseAndRoleIn(roles);
 
        // All non-deleted
        assertTrue(result.stream().allMatch(u -> !u.isDeleted()));
        // All in allowed roles
        assertTrue(result.stream().allMatch(u -> roles.contains(u.getRole())));
 
        // Focus only our dept301 created ones: should include Active Emp + Active Reviewer, exclude Active Admin
        List<User> ours = result.stream()
                .filter(u -> u.getDepartment() != null && u.getDepartment().getDeptId() != null
&& u.getDepartment().getDeptId().equals(301))
                .toList();
 
        assertEquals(2, ours.size());
        assertTrue(ours.stream().anyMatch(u -> u.getEmail().equals("active.emp@test.com")));
        assertTrue(ours.stream().anyMatch(u -> u.getEmail().equals("active.reviewer@test.com")));
        assertFalse(ours.stream().anyMatch(u -> u.getEmail().equals("active.admin@test.com")));
    }

 
    // ------------------------------------------------------------
    // 5) findByRoleAndDepartment_DeptIdAndDeletedFalse  - Advait
    // ------------------------------------------------------------
    @Test
    @DisplayName("Find active Reviewers by Department ID")
    void testFindByRoleAndDepartment_DeptIdAndDeletedFalse() {
 
        Department dept = new Department();
        dept.setDeptId(1);
        dept.setDeptName("Engineering");
        Department savedDept = entityManager.persistFlushFind(dept);
 
        User reviewer = User.builder().name("Reviewer One").email("reviewer@test.com").role(Constants.Role.REVIEWER).department(savedDept).deleted(false).build();
 
        User employee = User.builder().name("Employee One").email("employee@test.com").role(Constants.Role.EMPLOYEE).department(savedDept).deleted(false).build();
 
        entityManager.persist(reviewer);
        entityManager.persist(employee);
        entityManager.flush();
 
        List<User> results = userRepo.findByRoleAndDepartment_DeptIdAndDeletedFalse(Constants.Role.REVIEWER, savedDept.getDeptId());
 
        assertEquals(1, results.size(), "Should only find the active Reviewer");
        assertEquals("Reviewer One", results.get(0).getName());
    }
}