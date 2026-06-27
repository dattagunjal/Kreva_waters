package com.mineralwater.service;

import com.mineralwater.dto.AuthDto.*;
import com.mineralwater.model.OtpEntry;
import com.mineralwater.model.User;
import com.mineralwater.repository.UserRepository;
import com.mineralwater.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern MOBILE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    // ── helpers ────────────────────────────────────────────────────────────────

    private boolean isEmail(String s)  { return EMAIL_PATTERN.matcher(s).matches(); }
    private boolean isMobile(String s) { return MOBILE_PATTERN.matcher(s).matches(); }

    private User findByLoginId(String loginId) {
        if (isEmail(loginId))
            return userRepository.findByEmail(loginId)
                    .orElseThrow(() -> new RuntimeException("No account found for this email."));
        if (isMobile(loginId))
            return userRepository.findByMobileNumber(loginId)
                    .orElseThrow(() -> new RuntimeException("No account found for this mobile number."));
        throw new RuntimeException("Please enter a valid email address or 10-digit mobile number.");
    }

    private AuthResponse toResponse(String token, User user) {
        return new AuthResponse(token, user);
    }

    // ── OTP dispatch ──────────────────────────────────────────────────────────

    /**
     * Validates the loginId format, then delegates to OtpService.
     * Returns the generated code ONLY so the controller can include
     * it in X-Dev-OTP when mock mode is active.
     */
    public String sendOtp(String loginId, String purposeStr) {
        String id = loginId.trim();
        if (!isEmail(id) && !isMobile(id))
            throw new RuntimeException(
                    "Please enter a valid email address or 10-digit mobile number.");

        OtpEntry.Purpose purpose = OtpEntry.Purpose.valueOf(purposeStr.toUpperCase());

        // For LOGIN OTPs the user must already have an account
        if (purpose == OtpEntry.Purpose.LOGIN) findByLoginId(id);

        return otpService.sendOtp(id, purpose);
    }

    // ── Registration ──────────────────────────────────────────────────────────

    public AuthResponse register(RegisterRequest req) {
        String loginId = req.getLoginId().trim();

        // Verify the OTP before creating the account
        otpService.verifyOtp(loginId, req.getOtp(), OtpEntry.Purpose.REGISTER);

        User.UserBuilder builder = User.builder()
                .name(req.getName())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(User.Role.USER);

        // Set and validate Email if provided
        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String email = req.getEmail().trim();
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("An account with this email already exists.");
            }
            builder.email(email);
        }

        // Set and validate Mobile Number if provided
        if (req.getMobileNumber() != null && !req.getMobileNumber().isBlank()) {
            String mobile = req.getMobileNumber().trim();
            if (userRepository.existsByMobileNumber(mobile)) {
                throw new RuntimeException("An account with this mobile number already exists.");
            }
            builder.mobileNumber(mobile);
        }

        // Ensure at least one was set
        if ((req.getEmail() == null || req.getEmail().isBlank()) &&
            (req.getMobileNumber() == null || req.getMobileNumber().isBlank())) {
            throw new RuntimeException("Either email or mobile number must be provided.");
        }

        User saved = userRepository.save(builder.build());
        String token = jwtUtil.generateToken(saved.getPrincipal());
        return toResponse(token, saved);
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        String loginId = req.getLoginId().trim();
        User user = findByLoginId(loginId);

        boolean usingOtp      = req.getOtp()      != null && !req.getOtp().isBlank();
        boolean usingPassword = req.getPassword() != null && !req.getPassword().isBlank();

        if (usingOtp) {
            // ── OTP login path ──────────────────────────────────────────────
            otpService.verifyOtp(loginId, req.getOtp(), OtpEntry.Purpose.LOGIN);
        } else if (usingPassword) {
            // ── Password login path ─────────────────────────────────────────
            if (user.getPassword() == null || user.getPassword().isBlank())
                throw new RuntimeException(
                        "This account has no password set. Please log in with OTP.");
            if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
                throw new RuntimeException("Incorrect password. Please try again.");
        } else {
            throw new RuntimeException("Please provide either a password or an OTP to log in.");
        }

        String token = jwtUtil.generateToken(user.getPrincipal());
        return toResponse(token, user);
    }
}
