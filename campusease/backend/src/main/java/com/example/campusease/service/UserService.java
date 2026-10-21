package com.example.campusease.service;

import com.example.campusease.dto.AuthResponse;
import com.example.campusease.dto.LoginRequest;
import com.example.campusease.dto.ProfileUpdateRequest;
import com.example.campusease.dto.RegisterRequest;
import com.example.campusease.exception.BadRequestException;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.AuthProvider;
import com.example.campusease.model.Role;
import com.example.campusease.model.User;
import com.example.campusease.repository.UserRepository;
import com.example.campusease.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
private final PasswordEncoder passwordEncoder;
private final JwtUtil jwtUtil;
private final EmailService emailService;

public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                    JwtUtil jwtUtil, EmailService emailService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.emailService = emailService;
}

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.ROLE_STUDENT);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());
        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }

   public AuthResponse login(LoginRequest request) {
    User user = userRepository.findByUsername(request.getUsernameOrEmail())
            .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
            .orElseThrow(() -> new BadRequestException("Invalid email/username or password."));

    if (user.getPassword() == null) {
        throw new BadRequestException(
            "This account signs in with Google -- use that button instead of a password.");
    }

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new BadRequestException("Invalid email/username or password.");
    }

    String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());
    return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
}

    public User createVendor(String username, String email, String rawPassword, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRole(Role.ROLE_VENDOR);
        return userRepository.save(user);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public User findOrCreateOAuthUser(String email, String displayName, AuthProvider provider) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setUsername(generateUniqueUsername(email, displayName));
            user.setPassword(null);
            user.setRole(Role.ROLE_STUDENT);
            user.setAuthProvider(provider);
            return userRepository.save(user);
        });
    }

    private String generateUniqueUsername(String email, String displayName) {
        String base = (displayName != null && !displayName.isBlank())
                ? displayName.trim().toLowerCase().replaceAll("[^a-z0-9]+", "")
                : email.substring(0, email.indexOf('@')).toLowerCase().replaceAll("[^a-z0-9]+", "");
        if (base.isBlank()) base = "user";
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        if (user.getPassword() == null) {
            return;
        }
        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);
        emailService.sendPasswordResetEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has already been used."));
        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This reset link has expired. Please request a new one.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public User updateProfile(String username, ProfileUpdateRequest request) {
        User user = getByUsername(username);
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BadRequestException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }
        return userRepository.save(user);
    }
}