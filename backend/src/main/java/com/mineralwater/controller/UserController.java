package com.mineralwater.controller;

import com.mineralwater.model.User;
import com.mineralwater.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .or(() -> userRepository.findByMobileNumber(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(
            @RequestBody ProfileUpdateRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .or(() -> userRepository.findByMobileNumber(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

        // Validate and update email if changed
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String newEmail = req.getEmail().trim();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new RuntimeException("Email address already in use.");
                }
                user.setEmail(newEmail);
            }
        } else {
            user.setEmail(null);
        }

        // Validate and update mobile number if changed
        if (req.getMobileNumber() != null && !req.getMobileNumber().isBlank()) {
            String newMobile = req.getMobileNumber().trim();
            if (!newMobile.equals(user.getMobileNumber())) {
                if (userRepository.existsByMobileNumber(newMobile)) {
                    throw new RuntimeException("Mobile number already in use.");
                }
                user.setMobileNumber(newMobile);
            }
        } else {
            user.setMobileNumber(null);
        }

        if (user.getEmail() == null && user.getMobileNumber() == null) {
            throw new RuntimeException("Either email or mobile number must be provided.");
        }

        user.setName(req.getName());
        user.setAddress(req.getAddress());
        user.setProfileImage(req.getProfileImage());

        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .or(() -> userRepository.findByMobileNumber(userDetails.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect current password.");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }

    @Data
    public static class ProfileUpdateRequest {
        private String name;
        private String email;
        private String mobileNumber;
        private String address;
        private String profileImage;
    }

    @Data
    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
    }
}
