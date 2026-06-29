package com.stylemind.auth.entity;

import com.stylemind.common.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "email", length = 100, unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "provider", length = 20, nullable = false)
    private String provider = "LOCAL";

    @Column(name = "provider_id", length = 100)
    private String providerId;

    @Column(name = "role", length = 20, nullable = false)
    private String role = "CUSTOMER";

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "password_setup_required", nullable = false)
    @Builder.Default
    private Boolean passwordSetupRequired = false;

    @Column(name = "password_setup_token_hash", length = 255)
    private String passwordSetupTokenHash;

    @Column(name = "password_setup_token_expires_at")
    private java.time.LocalDateTime passwordSetupTokenExpiresAt;

    @Column(name = "password_reset_otp_hash", length = 255)
    private String passwordResetOtpHash;

    @Column(name = "password_reset_otp_expires_at")
    private java.time.LocalDateTime passwordResetOtpExpiresAt;

    @Column(name = "password_reset_otp_attempts")
    @Builder.Default
    private Integer passwordResetOtpAttempts = 0;

    @Column(name = "password_reset_requested_at")
    private java.time.LocalDateTime passwordResetRequestedAt;

    @Column(name = "password_reset_token_hash", length = 255)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_token_expires_at")
    private java.time.LocalDateTime passwordResetTokenExpiresAt;
}
