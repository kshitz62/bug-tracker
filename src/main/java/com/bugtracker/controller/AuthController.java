package com.bugtracker.controller;

import com.bugtracker.dto.AuthResponse;
import com.bugtracker.dto.LoginRequest;
import com.bugtracker.dto.RegisterRequest;
import com.bugtracker.dto.UserResponse;
import com.bugtracker.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Authentication endpoints. All responses are JSON and are documented in the README.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Registers a TESTER or DEVELOPER account and returns a ready to use token. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Returns the profile of the caller, used to restore a session on page load. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser() {
        return ResponseEntity.ok(authService.currentUser());
    }

    /**
     * Tokens are stateless, so logout simply instructs the client to discard its
     * token. The endpoint exists so that the UI can perform a symmetric call and
     * so that logging/monitoring can observe sign-out events.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Signed out successfully."));
    }
}
