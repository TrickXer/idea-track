package com.ideatrack.main.controller;

import com.ideatrack.main.config.JwtUtil;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.data.User;
import com.ideatrack.main.dto.AuthRequest;
import com.ideatrack.main.dto.AuthResponse;
import com.ideatrack.main.exception.DuplicateEmailException;
import com.ideatrack.main.exception.ForbiddenOperationException;
import com.ideatrack.main.exception.UnauthorizedException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.service.MyUserDetailsService;
import com.ideatrack.main.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TestAuthController {

    @Autowired
    private AuthController controller;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private BCryptPasswordEncoder passwordEncoder;
    @MockitoBean private MyUserDetailsService myUserDetailsService;

    // ------------------------------------------------------------
    // 1) POST /api/auth/signup
    // ------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/signup - Success")
    void signup_ok() {
        AuthRequest req = new AuthRequest();
        req.setName("Emp");
        req.setEmail("emp@company.com");
        req.setPassword("Strong#123A");
        req.setRole("EMPLOYEE");

        User saved = new User();
        saved.setEmail("emp@company.com");

        when(userService.registerUserFromAuth(any(AuthRequest.class)))
                .thenReturn(saved);

        ResponseEntity<?> resp = controller.signup(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isEqualTo("User registered successfully");

        verify(userService).registerUserFromAuth(any(AuthRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/signup - DuplicateEmailException thrown")
    void signup_duplicateEmail_exceptionThrown() {
        AuthRequest req = new AuthRequest();
        req.setEmail("emp@company.com");
        req.setPassword("Strong#123A");

        when(userService.registerUserFromAuth(any(AuthRequest.class)))
                .thenThrow(new DuplicateEmailException("Email is already in use!"));

        assertThatThrownBy(() -> controller.signup(req))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email is already in use!");

        verify(userService).registerUserFromAuth(any(AuthRequest.class));
    }

    // ------------------------------------------------------------
    // 2) POST /api/auth/login
    // ------------------------------------------------------------

    @Test
    @DisplayName("POST /api/auth/login - Success")
    void login_ok() {
        AuthRequest req = new AuthRequest();
        req.setEmail("emp@company.com");
        req.setPassword("Strong#123A");

        User u = new User();
        u.setEmail("emp@company.com");
        u.setPassword("$bcrypt");
        u.setStatus(Constants.Status.ACTIVE);
        u.setDeleted(false);

        when(userService.findByEmail("emp@company.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Strong#123A", "$bcrypt")).thenReturn(true);

        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("emp@company.com")
                .password("$bcrypt")
                .authorities("EMPLOYEE")
                .build();

        when(myUserDetailsService.loadUserByUsername("emp@company.com")).thenReturn(details);
        when(jwtUtil.generateToken(details)).thenReturn("jwt-token");

        ResponseEntity<?> resp = controller.login(req);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isInstanceOf(AuthResponse.class);

        AuthResponse body = (AuthResponse) resp.getBody();
        assertThat(body.getToken()).isEqualTo("jwt-token");
        assertThat(body.getMessage()).contains("Login successful");

        verify(jwtUtil).generateToken(details);
    }

    @Test
    @DisplayName("POST /api/auth/login - INACTIVE or deleted -> ForbiddenOperationException thrown")
    void login_inactive_or_deleted_forbidden_exceptionThrown() {
        AuthRequest req = new AuthRequest();
        req.setEmail("emp@company.com");
        req.setPassword("Strong#123A");

        User u = new User();
        u.setEmail("emp@company.com");
        u.setPassword("$bcrypt");
        u.setStatus(Constants.Status.INACTIVE);
        u.setDeleted(false);

        when(userService.findByEmail("emp@company.com")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> controller.login(req))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("inactive or deleted");

        // Ensure password check not done
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid credentials -> UnauthorizedException thrown")
    void login_invalidCredentials_unauthorized_exceptionThrown() {
        AuthRequest req = new AuthRequest();
        req.setEmail("emp@company.com");
        req.setPassword("Wrong#123A");

        User u = new User();
        u.setEmail("emp@company.com");
        u.setPassword("$bcrypt");
        u.setStatus(Constants.Status.ACTIVE);
        u.setDeleted(false);

        when(userService.findByEmail("emp@company.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Wrong#123A", "$bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(req))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("POST /api/auth/login - User not found -> UserNotFound thrown")
    void login_userNotFound_exceptionThrown() {
        AuthRequest req = new AuthRequest();
        req.setEmail("missing@company.com");
        req.setPassword("Strong#123A");

        when(userService.findByEmail("missing@company.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.login(req))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}