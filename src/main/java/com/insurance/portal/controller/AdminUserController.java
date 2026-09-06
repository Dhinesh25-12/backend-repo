package com.insurance.portal.controller;

import com.insurance.portal.dto.request.UpdateUserRolesRequest;
import com.insurance.portal.dto.request.UpdateUserStatusRequest;
import com.insurance.portal.dto.response.UserResponse;
import com.insurance.portal.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-facing aliases for user management. These mirror {@link UserController}'s
 * existing behavior under the "/api/admin/users" path expected by the frontend.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin user management (alias routes)")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(userService.updateUserRoles(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(userService.setUserActive(id, request.enabled()));
    }
}
