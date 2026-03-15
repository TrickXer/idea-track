package com.ideatrack.main.controller;
import com.ideatrack.main.dto.*;
import com.ideatrack.main.dto.UserCreateResponse;
import com.ideatrack.main.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPERADMIN')")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    // POST http://localhost:8091/api/admin/create
    @PostMapping("/create")
    public ResponseEntity<UserCreateResponse> createUser(
            Principal principal,
            @RequestBody UserCreateRequest request
    ) {
        UserCreateResponse response =
                userService.createUserByOperator(principal.getName(), request);
        return ResponseEntity.ok(response);
    }

    // GET http://localhost:8091/api/admin/all
    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers(Principal principal) {
        return ResponseEntity.ok(
                userService.getAllUsersForOperator(principal.getName())
        );
    }

    // PUT http://localhost:8091/api/admin/{id}
    @PutMapping("/{id}")
    public ResponseEntity<UserUpdateResponse> updateUser(
            Principal principal,
            @PathVariable Integer id,
            @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(
                userService.updateUserByOperator(principal.getName(), id, request)
        );
    }

    // DELETE http://localhost:8091/api/admin/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiMessageResponse> deleteUser(
            Principal principal,
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                userService.deleteUserByOperator(principal.getName(), id)
        );
    }
}