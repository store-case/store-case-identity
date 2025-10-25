package com.leedahun.storecaseidentity.domain.auth.entity;

import com.leedahun.storecaseidentity.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Enumerated(EnumType.STRING)
    private EmailPurpose purpose;

    @Column(length = 6)
    private String code;

    private LocalDateTime expiresAt;

    @Builder.Default
    private Integer attemptCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private EmailVerifyStatus status =  EmailVerifyStatus.PENDING;

    private LocalDateTime lockedUntil;

    public void updateStatus(EmailVerifyStatus status) {
        this.status = status;
    }

    public void updateLockedUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    public void resetAttemptCount() {
        this.attemptCount = 0;
    }

    public void clearLockedUntil() {
        this.lockedUntil = null;
    }

    public void updateCode(String code) {
        this.code = code;
    }

    public void resetExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
