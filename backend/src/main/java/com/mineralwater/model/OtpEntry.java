package com.mineralwater.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Stores pending OTPs for registration and passwordless login.
 * Each row is keyed by the loginId (email or mobile) + purpose.
 * Expired / used entries can be cleaned up by a scheduled job in production.
 */
@Entity
@Table(name = "otp_entries")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The email address or mobile number the OTP was sent to. */
    @Column(nullable = false)
    private String loginId;

    @Column(nullable = false, length = 6)
    private String code;

    /**
     * Purpose distinguishes registration OTPs from login OTPs so
     * they cannot be cross-used.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private boolean used = false;

    public enum Purpose { REGISTER, LOGIN }
}
