package com.example.campusease.controller;

import com.example.campusease.dto.AuthResponse;
import com.example.campusease.dto.ForgotPasswordRequest;
import com.example.campusease.dto.LoginRequest;
import com.example.campusease.dto.ProfileUpdateRequest;
import com.example.campusease.dto.RegisterRequest;
import com.example.campusease.dto.ResetPasswordRequest;
import com.example.campusease.model.User;
import com.example.campusease.security.CustomUserDetails;
import com.example.campusease.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "If that email is registered, a reset link has been sent."));
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated. You can now log in with your new password."));
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = userService.getByUsername(principal.getUsername());
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(@AuthenticationPrincipal CustomUserDetails principal,
                                               @RequestBody ProfileUpdateRequest request) {
        User user = userService.updateProfile(principal.getUsername(), request);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }
}