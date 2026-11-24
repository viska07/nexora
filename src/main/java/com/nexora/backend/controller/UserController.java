package com.nexora.backend.controller;

import com.nexora.backend.dto.AdminUserResponse;
import com.nexora.backend.dto.AdminUserUpdateRequest;
import com.nexora.backend.entity.Role;
import com.nexora.backend.entity.User;
import com.nexora.backend.service.RoleService;
import com.nexora.backend.service.UserService;

import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    public UserController(
            UserService userService,
            RoleService roleService
    ) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> getAllUsers() {

        List<AdminUserResponse> response = userService
                .getAllUsers()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id
    ) {

        User user = userService
                .getUserById(id)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "User not found"
                    ));
        }

        return ResponseEntity.ok(toResponse(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {

        User user = userService
                .getUserById(id)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "User not found"
                    ));
        }

        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userService.emailExists(request.getEmail())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "Email is already registered"
                    ));
        }

        Role role = roleService
                .getRoleById(request.getRoleId())
                .orElse(null);

        if (role == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Role not found"
                    ));
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userService.saveUser(user);

        return ResponseEntity.ok(toResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id
    ) {

        User user = userService
                .getUserById(id)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "User not found"
                    ));
        }

        try {
            userService.deleteUser(id);

            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "User cannot be deleted because the user is still referenced by other data"
                    ));
        }
    }

    private AdminUserResponse toResponse(User user) {

        Role role = user.getRole();

        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                role != null ? role.getId() : null,
                role != null ? role.getName() : null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}