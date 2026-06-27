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

    /**
     * TODO (PRODUCTION): Replace this stub with a real SMS call.
     *
     * Example using Fast2SMS (already wired in NotificationService):
     * <pre>
     *   Map&lt;String, Object&gt; body = Map.of(
     *       "route",    "q",
     *       "message",  "Your Ugam Waters OTP is: " + code + ". Valid for " + expiryMinutes + " minutes.",
     *       "language", "english",
     *       "flash",    0,
     *       "numbers",  mobile
     *   );
     *   // POST to https://www.fast2sms.com/dev/bulkV2 with Authorization header = apiKey
     * </pre>
     */
    private void sendSmsOtp(String mobile, String code) {
        log.warn("[OTP] sendSmsOtp() — production stub not yet implemented for mobile={}", mobile);
        // Wire Fast2SMS / Twilio / MSG91 here
    }

    /**
     * TODO (PRODUCTION): Replace this stub with a real email call.
     *
     * Example using Spring Mail:
     * <pre>
     *   SimpleMailMessage msg = new SimpleMailMessage();
     *   msg.setTo(email);
     *   msg.setSubject("Your Ugam Waters OTP");
     *   msg.setText("Your OTP is: " + code + ". Valid for " + expiryMinutes + " minutes.");
     *   mailSender.send(msg);   // inject JavaMailSender
     * </pre>
     */
    private void sendEmailOtp(String email, String code) {
        log.warn("[OTP] sendEmailOtp() — production stub not yet implemented for email={}", email);
        // Wire JavaMailSender / SendGrid / AWS SES here
    }
}
