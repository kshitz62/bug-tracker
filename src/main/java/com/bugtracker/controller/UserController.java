package com.bugtracker.controller;

import com.bugtracker.dto.UserResponse;
import com.bugtracker.dto.UserRoleUpdateRequest;
import com.bugtracker.dto.UserStatusUpdateRequest;
import com.bugtracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User listing (for assignee pickers) and administrator user management.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Administrators only: full user directory. */
    @GetMapping
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** Users that may own a bug (developers and administrators). */
    @GetMapping("/assignable")
    public ResponseEntity<List<UserResponse>> listAssignableUsers() {
        return ResponseEntity.ok(userService.getAssignableUsers());
    }

    @GetMapping("/developers")
    public ResponseEntity<List<UserResponse>> listDevelopers() {
        return ResponseEntity.ok(userService.getDevelopers());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id,
                                                   @Valid @RequestBody UserRoleUpdateRequest request) {
        return ResponseEntity.ok(userService.updateRole(id, request.role()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                     @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(userService.updateEnabled(id, request.enabled()));
    }
}
