package com.mineralwater.dto;

import lombok.Data;

public class AuthDto {

    // ── Registration ─────────────────────────────────────────────────────────

    @Data
    public static class SendOtpRequest {
        private String loginId;       // email OR mobile number
        private String purpose;       // "REGISTER" | "LOGIN"
    }

    @Data
    public static class RegisterRequest {
        private String name;
        private String email;
        private String mobileNumber;
        private String loginId;       // verified email OR mobile number
        private String password;
        private String otp;           // must be verified before account creation
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        private String loginId;
        private String password;      // null / empty when using OTP login
        private String otp;           // null / empty when using password login
    }

    // ── Shared response ───────────────────────────────────────────────────────

    @Data
    public static class AuthResponse {
        private String token;
        private UserResponse user;

        @Data
        public static class UserResponse {
            private Long id;
            private String name;
            private String email;
            private String mobileNumber;
            private String role;
        }

        public AuthResponse(String token, com.mineralwater.model.User user) {
            this.token = token;
            this.user = new UserResponse();
            this.user.setId(user.getId());
            this.user.setName(user.getName());
            this.user.setEmail(user.getEmail());
            this.user.setMobileNumber(user.getMobileNumber());
            this.user.setRole(user.getRole().name());
        }
    }

    @Data
    public static class MessageResponse {
        private final String message;
    }
}
