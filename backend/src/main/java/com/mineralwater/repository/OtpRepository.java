package com.mineralwater.repository;

import com.mineralwater.model.OtpEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpEntry, Long> {

    Optional<OtpEntry> findTopByLoginIdAndPurposeAndUsedFalseOrderByExpiresAtDesc(
            String loginId, OtpEntry.Purpose purpose);

    /** Clean up all expired/used entries for a loginId before issuing a new OTP. */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpEntry o WHERE o.loginId = :loginId AND o.purpose = :purpose")
    void deleteAllByLoginIdAndPurpose(String loginId, OtpEntry.Purpose purpose);

    /** Scheduled cleanup hook — removes all globally expired entries. */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpEntry o WHERE o.expiresAt < :now")
    void deleteExpiredBefore(LocalDateTime now);
}
