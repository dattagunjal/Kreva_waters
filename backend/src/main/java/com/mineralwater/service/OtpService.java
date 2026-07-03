package com.mineralwater.service;

import com.mineralwater.model.OtpEntry;
import com.mineralwater.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Generates, delivers, and verifies 6-digit OTPs.
 *
 * ── MOCK MODE (otp.mock.enabled=true, for local dev) ─────────────────────────
 *   - OTP is printed to the Spring console log.
 *   - The generated code is also returned to the caller so the controller can
 *     set it in the response header X-Dev-OTP (makes Postman/frontend testing easy).
 *   - No real SMS or email is sent.
 *
 * ── PRODUCTION MODE (otp.mock.enabled=false) ──────────────────────────────────
 *   - For MOBILE numbers  → integrate Fast2SMS (already used for order alerts).
 *     Replace the sendSmsOtp() stub with the Fast2SMS bulk/transactional call.
 *   - For EMAIL addresses → integrate JavaMailSender (Spring Mail) or any
 *     transactional email provider (SendGrid, AWS SES, Mailgun).
 *     Replace the sendEmailOtp() stub with your chosen provider.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;

    @Value("${otp.expiry.minutes:10}")
    private int expiryMinutes;

    @Value("${otp.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${sms.api.key:}")
    private String smsApiKey;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:info@Kreva.in}")
    private String brevoSenderEmail;

    // ── public API ─────────────────────────────────────────────────────────────

    /**
     * Generates a new OTP for the given loginId and purpose, persists it, and
     * delivers it (mock or real).
     *
     * @return the generated code — always returned so the controller can expose
     *         it in X-Dev-OTP when mockEnabled=true; in production the controller
     *         MUST NOT include it in the response.
     */
    public String sendOtp(String loginId, OtpEntry.Purpose purpose) {
        // Invalidate any previous pending OTP for this loginId + purpose
        otpRepository.deleteAllByLoginIdAndPurpose(loginId, purpose);

        String code = generateCode();

        OtpEntry entry = OtpEntry.builder()
                .loginId(loginId)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();

        otpRepository.save(entry);

        if (mockEnabled) {
            // ── MOCK: print to console and return code ──────────────────────
            log.info("╔══════════════════════════════════════╗");
            log.info("║  [MOCK OTP] loginId : {}  ║", loginId);
            log.info("║  [MOCK OTP] purpose : {}        ║", purpose);
            log.info("║  [MOCK OTP] code    : {}              ║", code);
            log.info("╚══════════════════════════════════════╝");
        } else {
            // ── PRODUCTION: dispatch based on loginId type ──────────────────
            if (EMAIL_PATTERN.matcher(loginId).matches()) {
                sendEmailOtp(loginId, code);
            } else {
                sendSmsOtp(loginId, code);
            }
        }

        return code;   // caller uses this only when mockEnabled=true
    }

    /**
     * Verifies the supplied code for the given loginId and purpose.
     * Marks the entry as used on success so it cannot be replayed.
     *
     * @throws RuntimeException with a user-facing message on any failure
     */
    public void verifyOtp(String loginId, String code, OtpEntry.Purpose purpose) {
        OtpEntry entry = otpRepository
                .findTopByLoginIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(loginId, purpose)
                .orElseThrow(() -> new RuntimeException(
                        "No OTP found for this identifier. Please request a new one."));

        if (entry.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("OTP has expired. Please request a new one.");

        if (!entry.getCode().equals(code.trim()))
            throw new RuntimeException("Incorrect OTP. Please try again.");

        entry.setUsed(true);
        otpRepository.save(entry);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    private String generateCode() {
        // Guaranteed 6-digit code (100000–999999)
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private void sendSmsOtp(String mobile, String code) {
        if (smsApiKey == null || smsApiKey.isBlank()) {
            log.warn("[SMS Stub] SMS API key is not configured. Simulating SMS OTP to {}: {}", mobile, code);
            return;
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://www.fast2sms.com/dev/bulkV2";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("authorization", smsApiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            // Using the "q" (Quick SMS) route to bypass DLT / website verification requirements
            java.util.Map<String, Object> body = java.util.Map.of(
                "route", "q",
                "message", "Your Kreva verification code is: " + code,
                "language", "english",
                "flash", 0,
                "numbers", mobile
            );

            org.springframework.http.HttpEntity<java.util.Map<String, Object>> request = new org.springframework.http.HttpEntity<>(body, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ SMS OTP successfully sent to: {}", mobile);
            } else {
                log.error("❌ SMS OTP failed for: {}. Response: {}", mobile, response.getBody());
                throw new RuntimeException("SMS gateway error: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Error sending SMS OTP to: {}", mobile, e);
            throw new RuntimeException("Failed to send SMS OTP: " + e.getMessage());
        }
    }

    private void sendEmailOtp(String email, String code) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("[Email Brevo Stub] Brevo API key is not configured. Simulating Email OTP to {}: {}", email, code);
            return;
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("api-key", brevoApiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("sender", java.util.Map.of("name", "Kreva", "email", brevoSenderEmail));
            body.put("to", java.util.List.of(java.util.Map.of("email", email)));
            body.put("subject", "Your Kreva OTP Verification Code");
            body.put("textContent", "Your OTP verification code is: " + code + "\n\nThis code will expire in " + expiryMinutes + " minutes.");

            org.springframework.http.HttpEntity<java.util.Map<String, Object>> request = new org.springframework.http.HttpEntity<>(body, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Brevo Email OTP successfully sent to: {}", email);
            } else {
                log.error("❌ Brevo Email OTP failed for: {}. Response: {}", email, response.getBody());
                throw new RuntimeException("Brevo API error: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Error sending Brevo Email OTP to: {}", email, e);
            throw new RuntimeException("Failed to send Email OTP: " + e.getMessage());
        }
    }
}
