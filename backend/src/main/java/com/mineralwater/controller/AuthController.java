package com.mineralwater.controller;

import com.mineralwater.dto.AuthDto.*;
import com.mineralwater.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${otp.mock.enabled:true}")
    private boolean mockEnabled;

    /**
     * POST /api/auth/send-otp
     * Body: { "loginId": "...", "purpose": "REGISTER" | "LOGIN" }
     *
     * In mock mode: returns { "message": "OTP sent", "devOtp": "123456" }
     * In production: returns { "message": "OTP sent" } only — code is delivered via SMS/Email
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestBody SendOtpRequest req) {
        try {
            String generatedCode = authService.sendOtp(req.getLoginId(), req.getPurpose());

            if (mockEnabled) {
                return ResponseEntity.ok(Map.of(
                    "message", "OTP sent (mock mode — check server console or devOtp field)",
                    "devOtp",  generatedCode
                ));
            }
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "message", "Failed to send OTP: " + (e.getMessage() != null ? e.getMessage() : e.toString())
            ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }
}
