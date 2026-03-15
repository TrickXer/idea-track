package com.ideatrack.main.controller;

import com.ideatrack.main.dto.*;
import com.ideatrack.main.service.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestAdminController {

    @Autowired
    private AdminController controller;

    @MockitoBean
    private UserService userService;

    private Principal principal(String email) {
        return () -> email;
    }

    /**
     * Set an Authentication in the SecurityContext so @PreAuthorize can evaluate.
     */
    private void authenticateAs(String username, String... authorities) {
        var auths = java.util.Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(username, "N/A", auths);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------
    // 1) POST /api/admin/create
    // ------------------------------------------------------------
    @Test
    @DisplayName("POST /api/admin/create - Success")
    void createUser_ok() {
        // IMPORTANT: provide ADMIN authority (matches @PreAuthorize)
        authenticateAs("admin@company.com", "ADMIN");

        UserCreateRequest req = new UserCreateRequest();
        req.setName("Emp One");
        req.setEmail("emp1@company.com");
        req.setRole("EMPLOYEE");
        req.setDeptName("Engineering");

        UserResponse userResp = UserResponse.builder()
                .userId(101)
                .name("Emp One")
                .email("emp1@company.com")
                .role("EMPLOYEE")
                .deptName("Engineering")
                .deleted(false)
                .build();

        UserCreateResponse serviceResp = new UserCreateResponse(
                "User created successfully.",
                userResp,
                "Temp#123A"
        );

        when(userService.createUserByOperator(eq("admin@company.com"), any(UserCreateRequest.class)))
                .thenReturn(serviceResp);

        ResponseEntity<UserCreateResponse> resp =
                controller.createUser(principal("admin@company.com"), req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).contains("created successfully");
        assertThat(resp.getBody().getUser().getEmail()).isEqualTo("emp1@company.com");

        verify(userService).createUserByOperator(eq("admin@company.com"), any(UserCreateRequest.class));
    }

    // ------------------------------------------------------------
    // 2) GET /api/admin/all
    // ------------------------------------------------------------
    @Test
    @DisplayName("GET /api/admin/all - Success")
    void getAllUsers_ok() {
        authenticateAs("admin@company.com", "ADMIN");

        UserResponse u1 = UserResponse.builder()
                .userId(1).name("A").email("a@c.com").role("EMPLOYEE").deleted(false).build();
        UserResponse u2 = UserResponse.builder()
                .userId(2).name("B").email("b@c.com").role("REVIEWER").deleted(false).build();

        when(userService.getAllUsersForOperator("admin@company.com"))
                .thenReturn(List.of(u1, u2));

        ResponseEntity<List<UserResponse>> resp =
                controller.getAllUsers(principal("admin@company.com"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
        assertThat(resp.getBody().get(0).getEmail()).isEqualTo("a@c.com");

        verify(userService).getAllUsersForOperator("admin@company.com");
    }

    // ------------------------------------------------------------
    // 3) PUT /api/admin/{id}
    // ------------------------------------------------------------
    @Test
    @DisplayName("PUT /api/admin/{id} - Success")
    void updateUser_ok() {
        authenticateAs("admin@company.com", "ADMIN");

        UserUpdateRequest req = new UserUpdateRequest();
        req.setName("Updated Name");

        UserResponse updated = UserResponse.builder()
                .userId(101)
                .name("Updated Name")
                .email("emp1@company.com")
                .role("EMPLOYEE")
                .deleted(false)
                .build();

        UserUpdateResponse serviceResp = new UserUpdateResponse(
                "User updated successfully. Updated fields: name.",
                updated
        );

        when(userService.updateUserByOperator(eq("admin@company.com"), eq(101), any(UserUpdateRequest.class)))
                .thenReturn(serviceResp);

        ResponseEntity<UserUpdateResponse> resp =
                controller.updateUser(principal("admin@company.com"), 101, req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getUser().getName()).isEqualTo("Updated Name");

        verify(userService).updateUserByOperator(eq("admin@company.com"), eq(101), any(UserUpdateRequest.class));
    }

    // ------------------------------------------------------------
    // 4) DELETE /api/admin/{id}
    // ------------------------------------------------------------
    @Test
    @DisplayName("DELETE /api/admin/{id} - Success")
    void deleteUser_ok() {
        authenticateAs("admin@company.com", "ADMIN");

        ApiMessageResponse serviceResp =
                new ApiMessageResponse("User with ID 101 has been soft-deleted successfully.");

        when(userService.deleteUserByOperator("admin@company.com", 101))
                .thenReturn(serviceResp);

        ResponseEntity<ApiMessageResponse> resp =
                controller.deleteUser(principal("admin@company.com"), 101);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).contains("soft-deleted");

        verify(userService).deleteUserByOperator("admin@company.com", 101);
    }
}