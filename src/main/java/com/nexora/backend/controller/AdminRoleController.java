package com.nexora.backend.controller;

import com.nexora.backend.entity.Role;
import com.nexora.backend.service.RoleService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController {

    private final RoleService roleService;

    public AdminRoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(
                roleService.getAllRoles()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRoleById(
            @PathVariable Long id
    ) {
        return roleService
                .getRoleById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }
}