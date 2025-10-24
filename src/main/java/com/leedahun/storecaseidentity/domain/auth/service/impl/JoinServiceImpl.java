package com.leedahun.storecaseidentity.domain.auth.service.impl;

import com.leedahun.storecaseidentity.common.error.exception.EntityNotFoundException;
import com.leedahun.storecaseidentity.common.mail.EmailClient;
import com.leedahun.storecaseidentity.domain.auth.dto.EmailVerificationConfirmRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailPurpose;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerification;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerifyStatus;
import com.leedahun.storecaseidentity.domain.auth.exception.EmailVerificationAttemptLimitExceededException;
import com.leedahun.storecaseidentity.domain.auth.exception.EmailVerificationExpiredException;
import com.leedahun.storecaseidentity.domain.auth.exception.EmailVerificationLockedException;
import com.leedahun.storecaseidentity.domain.auth.repository.EmailVerificationRepository;
import com.leedahun.storecaseidentity.domain.auth.service.JoinService;
import com.leedahun.storecaseidentity.domain.auth.util.VerificationCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JoinServiceImpl implements JoinService {

    @Value("${spring.mail.lock_minutes}")
    private int lockMinutes;

    @Value("${spring.mail.expire_minutes}")
    private int expireMinutes;

    @Value("${spring.mail.max_attempts}")
    private int maxAttempts;

    private final EmailVerificationRepository emailVerificationRepository;
    private final SpringTemplateEngine templateEngine;
    private final EmailClient emailClient;

    @Override
    public void join(JoinRequestDto joinRequestDto) {
        // TODO REFACTOR
    }

    @Override
    public void sendJoinEmail(String email) {
        final LocalDateTime now = LocalDateTime.now();
        final String SUBJECT = "[StoreCase] 회원가입을 위한 이메일 인증번호입니다.";

        Optional<EmailVerification> optionalEmailVerification = emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(email, EmailPurpose.SIGNUP);

        if (optionalEmailVerification.isPresent()) {
            EmailVerification emailVerification = optionalEmailVerification.get();

            // 이미 인증 완료
            if (emailVerification.getStatus() == EmailVerifyStatus.VERIFIED) {
                return;
            }

            // 잠금중
            if (emailVerification.getStatus() == EmailVerifyStatus.LOCKED) {
                if (emailVerification.getLockedUntil() != null && now.isBefore(emailVerification.getLockedUntil())) {
                    throw new EmailVerificationLockedException();
                }
                // 잠금 해제 (지났으면 바로 초기화해서 계속 진행)
                emailVerification.updateStatus(EmailVerifyStatus.PENDING);
                emailVerification.resetAttemptCount();
                emailVerification.clearLockedUntil();
            }

            if (emailVerification.getStatus() == EmailVerifyStatus.PENDING && emailVerification.getExpiresAt().isAfter(now)) {
                // 새로운 코드 발급 & 이메일 전송
                String code = VerificationCodeUtil.generateEmailVerificationCode();
                emailVerification.updateCode(code);
                emailVerification.resetExpiresAt(now.plusMinutes(expireMinutes));
                emailVerificationRepository.save(emailVerification);

                String html = buildVerificationHtml(email, code);
                emailClient.sendOneEmail(email, SUBJECT, html);

                return;
            }

            String code = VerificationCodeUtil.generateEmailVerificationCode();
            String html = buildVerificationHtml(email, code);
            emailClient.sendOneEmail(email, SUBJECT, html);
        }

        String code = VerificationCodeUtil.generateEmailVerificationCode();
        EmailVerification emailVerification = EmailVerification.builder()
                .email(email)
                .purpose(EmailPurpose.SIGNUP)
                .code(code)
                .status(EmailVerifyStatus.PENDING)
                .attemptCount(0)
                .expiresAt(now.plusMinutes(expireMinutes))
                .build();
        emailVerificationRepository.save(emailVerification);

        String html = buildVerificationHtml(email, code);
        emailClient.sendOneEmail(email, SUBJECT, html);
    }

    private String buildVerificationHtml(String email, String code) {
        Context context = new Context();
        context.setVariable("brandName", "Store Case");
        context.setVariable("email", email);
        context.setVariable("code", code);
        context.setVariable("expiresMinutes", expireMinutes);
        context.setVariable("year", Year.now().getValue());

        return templateEngine.process("email/verification", context);
    }

    // 동시성 테스트 필요
    @Override
    public EmailVerificationConfirmResponseDto verifyEmailCode(EmailVerificationConfirmRequestDto emailVerificationConfirmRequestDto) {
        final LocalDateTime now =  LocalDateTime.now();

        // 회원의 인증번호 조회 status = PENDING purpose = SIGNUP
        EmailVerification emailVerification = emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(emailVerificationConfirmRequestDto.getEmail(), EmailPurpose.SIGNUP)
                .orElseThrow(() -> new EntityNotFoundException("EmailVerification",  emailVerificationConfirmRequestDto.getEmail()));

        // 리턴값 : 인증 시도 횟수(2/5), 인증 상태(PENDING, EXPIRED, LOCKED) -> 화면에서는 인증상태에 따른 분기 처리

        // 이미 검증된 경우
        if (emailVerification.getStatus().equals(EmailVerifyStatus.VERIFIED)) {
            return EmailVerificationConfirmResponseDto.from(emailVerification);
        }

        // 잠김상태일 경우
        if (emailVerification.getStatus() == EmailVerifyStatus.LOCKED) {
            LocalDateTime lockUntil = emailVerification.getLockedUntil();

            // 아직 잠금 유지
            if (lockUntil != null && now.isBefore(lockUntil)) {
                throw new EmailVerificationLockedException();
            }

            // 잠금 기간 지났으면 해제하고 계속 진행
            emailVerification.updateStatus(EmailVerifyStatus.PENDING);
            emailVerification.resetAttemptCount();

            // save는 나중에 한 번에
        }

        // 만료기간 지난경우
        if (emailVerification.getExpiresAt().isBefore(now)) {
            emailVerification.updateStatus(EmailVerifyStatus.EXPIRED);
            emailVerificationRepository.save(emailVerification);
            throw new EmailVerificationExpiredException();
        }

        // 인증번호 비교
        // 일치 status = VERIFIED return true
        // 불일치 return false
        if (emailVerification.getCode().equals(emailVerificationConfirmRequestDto.getCode())) {
            emailVerification.updateStatus(EmailVerifyStatus.VERIFIED);
            emailVerificationRepository.save(emailVerification);
            return EmailVerificationConfirmResponseDto.from(emailVerification);
        }

        emailVerification.increateAttemptCount();

        // 횟수 차감 (횟수가 5회 넘어갈시 예외 발생) status = LOCKED
        if (emailVerification.getAttemptCount() >= maxAttempts) {
            emailVerification.updateStatus(EmailVerifyStatus.LOCKED);
            emailVerification.updateLockedUntil();
            emailVerificationRepository.save(emailVerification);

            throw new EmailVerificationAttemptLimitExceededException();
        }

        emailVerificationRepository.save(emailVerification);

        return EmailVerificationConfirmResponseDto.from(emailVerification);
    }
}
