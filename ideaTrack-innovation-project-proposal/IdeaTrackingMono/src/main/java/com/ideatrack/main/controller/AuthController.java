package com.ideatrack.main.controller;

import com.ideatrack.main.config.JwtUtil;
import com.ideatrack.main.data.Constants;
import com.ideatrack.main.dto.AuthRequest;
import com.ideatrack.main.dto.AuthResponse;
import com.ideatrack.main.exception.ForbiddenOperationException;
import com.ideatrack.main.exception.UnauthorizedException;
import com.ideatrack.main.exception.UserNotFoundException;
import com.ideatrack.main.service.MyUserDetailsService;
import com.ideatrack.main.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final MyUserDetailsService myUserDetailsService;

    public AuthController(UserService userService, JwtUtil jwtUtil, 
                         BCryptPasswordEncoder passwordEncoder, MyUserDetailsService myUserDetailsService) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.myUserDetailsService = myUserDetailsService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody AuthRequest request) {
        userService.registerUserFromAuth(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {

        return userService.findByEmail(request.getEmail())
                .map(user -> {

                    if (user.getStatus() == Constants.Status.INACTIVE || user.isDeleted()) {
                        // 403 -> GlobalExceptionHandler handleForbidden()
                        throw new ForbiddenOperationException("User is inactive or deleted");
                    }

                    boolean matches = passwordEncoder.matches(request.getPassword(), user.getPassword());

                    if (!matches) {
                        // 401 -> GlobalExceptionHandler handleUnauthorized()
                        throw new UnauthorizedException("Invalid credentials");
                    }

                    UserDetails userDetails = myUserDetailsService.loadUserByUsername(user.getEmail());
                    String token = jwtUtil.generateToken(userDetails);

                    return ResponseEntity.ok(new AuthResponse(token, "Login successful. Welcome "));
                })
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

}