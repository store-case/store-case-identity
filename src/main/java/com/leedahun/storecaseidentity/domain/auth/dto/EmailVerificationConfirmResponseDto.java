package com.leedahun.storecaseidentity.domain.auth.dto;

import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerification;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerifyStatus;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationConfirmResponseDto {
    private EmailVerifyStatus status;  // 인증상태
    private int attempts;              // 시도횟수
    private LocalDateTime retryAt;     // 잠금 기간
    private LocalDateTime expiresAt;   // 만료 기간

    public static EmailVerificationConfirmResponseDto from(EmailVerification emailVerification) {
        return EmailVerificationConfirmResponseDto.builder()
                .status(emailVerification.getStatus())
                .attempts(emailVerification.getAttemptCount())
                .retryAt(emailVerification.getLockedUntil())
                .expiresAt(emailVerification.getExpiresAt())
                .build();
    }
}
