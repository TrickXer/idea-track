package com.ideatrack.main.service;

import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.Department;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.*;
import com.ideatrack.main.exception.DuplicateEmailException;
import com.ideatrack.main.repository.IDepartmentRepository;
import com.ideatrack.main.repository.IUserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestUserService {

    @Mock private IUserRepository userRepository;
    @Mock private IDepartmentRepository departmentRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private UserProfileRules profileRules;

    @InjectMocks private UserService userService;

    private User superAdmin;
    private User admin;
    private Department engineering;

    @BeforeEach
    void setUp() {
        engineering = new Department();
        engineering.setDeptId(10);
        engineering.setDeptName("Engineering");

        superAdmin = new User();
        superAdmin.setUserId(1);
        superAdmin.setEmail("super@company.com");
        superAdmin.setRole(Constants.Role.SUPERADMIN);
        superAdmin.setStatus(Constants.Status.ACTIVE);
        superAdmin.setDeleted(false);

        admin = new User();
        admin.setUserId(2);
        admin.setEmail("admin@company.com");
        admin.setRole(Constants.Role.ADMIN);
        admin.setStatus(Constants.Status.ACTIVE);
        admin.setDeleted(false);

        lenient().when(profileRules.isProfileCompleted(any())).thenReturn(true);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$enc");
    }

    // ------------------------------------------------------------
    // findByEmail
    // ------------------------------------------------------------
    @Test
    @DisplayName("findByEmail: returns Optional from repository")
    void findByEmail_ok() {
        when(userRepository.findByEmail("a@c.com")).thenReturn(Optional.of(new User()));
        assertThat(userService.findByEmail("a@c.com")).isPresent();
        verify(userRepository).findByEmail("a@c.com");
    }

    // ------------------------------------------------------------
    // getAllDepartmentNames
    // ------------------------------------------------------------
    @Test
    @DisplayName("getAllDepartmentNames: returns sorted department names (case-insensitive)")
    void getAllDepartmentNames_ok() {
        Department d1 = new Department(); d1.setDeptName("engineering");
        Department d2 = new Department(); d2.setDeptName("Finance");
        Department d3 = new Department(); d3.setDeptName("HR");

        when(departmentRepository.findAllByDeletedFalse()).thenReturn(List.of(d1, d2, d3));

        DepartmentListResponse resp = userService.getAllDepartmentNames();

        assertThat(resp.getDeptNames()).containsExactly("engineering", "Finance", "HR");
        verify(departmentRepository).findAllByDeletedFalse();
    }

    // ------------------------------------------------------------
    // registerUserFromAuth
    // ------------------------------------------------------------
    @Test
    @DisplayName("registerUserFromAuth: creates user when email not used and password strong")
    void registerUserFromAuth_ok() {
        AuthRequest req = new AuthRequest();
        req.setName("Emp");
        req.setEmail("emp@company.com");
        req.setPassword("Strong#123A");
        req.setRole("EMPLOYEE");
        req.setDeptName("Engineering");

        when(userRepository.existsByEmail("emp@company.com")).thenReturn(false);
        when(departmentRepository.findByDeptNameIgnoreCaseAndDeletedFalse("Engineering"))
                .thenReturn(Optional.of(engineering));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User saved = userService.registerUserFromAuth(req);

        assertThat(saved.getEmail()).isEqualTo("emp@company.com");
        assertThat(saved.getDepartment()).isNotNull();
        assertThat(saved.getDepartment().getDeptName()).isEqualTo("Engineering");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("registerUserFromAuth: throws DuplicateEmailException when email already exists")
    void registerUserFromAuth_emailExists() {
        AuthRequest req = new AuthRequest();
        req.setEmail("emp@company.com");
        req.setPassword("Strong#123A");

        when(userRepository.existsByEmail("emp@company.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUserFromAuth(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    @DisplayName("registerUserFromAuth: throws when password weak")
    void registerUserFromAuth_weakPassword() {
        AuthRequest req = new AuthRequest();
        req.setEmail("emp@company.com");
        req.setPassword("weak"); // too weak

        when(userRepository.existsByEmail("emp@company.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.registerUserFromAuth(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password");
    }

    // ------------------------------------------------------------
    // createUserByOperator
    // ------------------------------------------------------------
    @Test
    @DisplayName("createUserByOperator: SUPERADMIN can create EMPLOYEE and returns temp password")
    void createUserByOperator_ok() {
        UserCreateRequest req = new UserCreateRequest();
        req.setName("Emp1");
        req.setEmail("emp1@company.com");
        req.setRole("EMPLOYEE");
        req.setDeptName("Engineering");

        when(userRepository.findByEmail("super@company.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.existsByEmail("emp1@company.com")).thenReturn(false);
        when(departmentRepository.findByDeptNameIgnoreCaseAndDeletedFalse("Engineering"))
                .thenReturn(Optional.of(engineering));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(101);
            return u;
        });

        UserCreateResponse resp = userService.createUserByOperator("super@company.com", req);

        assertThat(resp.getMessage()).contains("User created successfully");
        assertThat(resp.getUser().getEmail()).isEqualTo("emp1@company.com");
        assertThat(resp.getTempPassword()).isNotBlank();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUserByOperator: throws when email missing")
    void createUserByOperator_emailMissing() {
        UserCreateRequest req = new UserCreateRequest();
        req.setName("Emp1");
        req.setEmail("   ");

        when(userRepository.findByEmail("super@company.com")).thenReturn(Optional.of(superAdmin));

        assertThatThrownBy(() -> userService.createUserByOperator("super@company.com", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is required");
    }

    @Test
    @DisplayName("createUserByOperator: throws DuplicateEmailException when email already exists")
    void createUserByOperator_duplicateEmail() {
        UserCreateRequest req = new UserCreateRequest();
        req.setName("Emp1");
        req.setEmail("emp1@company.com");
        req.setRole("EMPLOYEE");
        req.setDeptName("Engineering");

        when(userRepository.findByEmail("super@company.com")).thenReturn(Optional.of(superAdmin));
        when(userRepository.existsByEmail("emp1@company.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUserByOperator("super@company.com", req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    @DisplayName("createUserByOperator: ADMIN cannot create ADMIN")
    void createUserByOperator_adminCannotCreateAdmin() {
        UserCreateRequest req = new UserCreateRequest();
        req.setName("Admin2");
        req.setEmail("admin2@company.com");
        req.setRole("ADMIN");

        when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmail("admin2@company.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.createUserByOperator("admin@company.com", req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ADMIN cannot create another ADMIN");
    }

    // ------------------------------------------------------------
    // updateUserByOperator
    // ------------------------------------------------------------
    @Test
    @DisplayName("updateUserByOperator: updates fields and returns message")
    void updateUserByOperator_ok() {
        User target = new User();
        target.setUserId(200);
        target.setName("Old");
        target.setEmail("old@company.com");
        target.setRole(Constants.Role.EMPLOYEE);
        target.setDeleted(false);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setName("New");
        req.setPhoneNo("+91-9000000000");

        when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(200)).thenReturn(Optional.of(target));

        // IMPORTANT: In your service code you added existsByEmail(req.getEmail()) early.
        // Here req.getEmail() is null, so existsByEmail(null) will be called.
        // To avoid failing due to strict mocks, we return false for any string (incl null).
        when(userRepository.existsByEmail(any())).thenReturn(false);

        when(userRepository.save(target)).thenReturn(target);

        UserUpdateResponse resp = userService.updateUserByOperator("admin@company.com", 200, req);

        assertThat(resp.getMessage()).contains("Updated fields");
        assertThat(resp.getUser().getName()).isEqualTo("New");
        verify(userRepository).save(target);
    }

    @Test
    @DisplayName("updateUserByOperator: throws DuplicateEmailException when email already exists")
    void updateUserByOperator_duplicateEmail() {
        User target = new User();
        target.setUserId(200);
        target.setName("Old");
        target.setEmail("old@company.com");
        target.setRole(Constants.Role.EMPLOYEE);
        target.setDeleted(false);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setEmail("new@company.com");

        when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(200)).thenReturn(Optional.of(target));
        when(userRepository.existsByEmail("new@company.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUserByOperator("admin@company.com", 200, req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email is already in use");
    }

    @Test
    @DisplayName("updateUserByOperator: throws when trying to set SUPERADMIN via update")
    void updateUserByOperator_cannotAssignSuperadmin() {
        User target = new User();
        target.setUserId(200);
        target.setRole(Constants.Role.EMPLOYEE);
        target.setDeleted(false);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setRole("SUPERADMIN");

        when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(200)).thenReturn(Optional.of(target));
        when(userRepository.existsByEmail(any())).thenReturn(false);

        assertThatThrownBy(() -> userService.updateUserByOperator("admin@company.com", 200, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot assign SUPERADMIN role");
    }

    // ------------------------------------------------------------
    // deleteUserByOperator
    // ------------------------------------------------------------
    @Test
    @DisplayName("deleteUserByOperator: soft deletes and returns message")
    void deleteUserByOperator_ok() {
        User target = new User();
        target.setUserId(200);
        target.setRole(Constants.Role.EMPLOYEE);
        target.setDeleted(false);
        target.setStatus(Constants.Status.ACTIVE);

        when(userRepository.findByEmail("admin@company.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(200)).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenReturn(target);

        ApiMessageResponse resp = userService.deleteUserByOperator("admin@company.com", 200);

        assertThat(target.isDeleted()).isTrue();
        assertThat(target.getStatus()).isEqualTo(Constants.Status.INACTIVE);
        assertThat(resp.getMessage()).contains("soft-deleted");
        verify(userRepository).save(target);
    }
}