package com.stylemind.auth.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A sign-up that has submitted email + password but not yet confirmed the
 * email OTP. Held separately from {@link User} so the verified-account, login,
 * admin and forgot-password flows are unaffected. Promoted to a real ACTIVE
 * {@link User} (and deleted) once {@code /register/verify-otp} succeeds.
 */
@Entity
@Table(name = "pending_registrations")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingRegistration extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    /** Already BCrypt-hashed; the raw password is never stored. */
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    /** BCrypt hash of the 6-digit OTP; the raw OTP is never stored. */
    @Column(name = "otp_hash", length = 255, nullable = false)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_attempts", nullable = false)
    @Builder.Default
    private Integer otpAttempts = 0;

    /** When the current OTP was issued; drives the resend cooldown. */
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
}
