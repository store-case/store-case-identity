package com.leedahun.storecaseidentity.domain.auth.service.impl;

import com.leedahun.storecaseidentity.common.error.exception.EntityNotFoundException;
import com.leedahun.storecaseidentity.common.mail.EmailClient;
import com.leedahun.storecaseidentity.domain.auth.dto.EmailVerificationConfirmRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailPurpose;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerification;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerifyStatus;
import com.leedahun.storecaseidentity.domain.auth.entity.User;
import com.leedahun.storecaseidentity.domain.auth.exception.EmailVerificationAlreadyDoneException;
import com.leedahun.storecaseidentity.domain.auth.exception.EmailVerificationAttemptLimitExceededException;
import com.leedahun.storecaseidentity.domain.auth.exception.EmailVerificationExpiredException;
import com.leedahun.storecaseidentity.domain.auth.exception.EmailVerificationLockedException;
import com.leedahun.storecaseidentity.domain.auth.exception.UserAlreadyExistsException;
import com.leedahun.storecaseidentity.domain.auth.repository.EmailVerificationRepository;
import com.leedahun.storecaseidentity.domain.auth.repository.UserRepository;
import com.leedahun.storecaseidentity.domain.auth.service.JoinService;
import com.leedahun.storecaseidentity.domain.auth.util.VerificationCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class JoinServiceImpl implements JoinService {

    @Value("${spring.mail.lock_minutes}")
    private int lockMinutes;

    @Value("${spring.mail.expire_minutes}")
    private int expireMinutes;

    @Value("${spring.mail.max_attempts}")
    private int maxAttempts;

    private static final String SUBJECT = "[StoreCase] 회원가입을 위한 이메일 인증번호입니다.";

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SpringTemplateEngine templateEngine;
    private final EmailClient emailClient;

    @Override
    @Transactional
    public void join(JoinRequestDto joinRequestDto) {
        userRepository.findByEmail(joinRequestDto.getEmail())
                .ifPresent(u -> {
                    throw new UserAlreadyExistsException();
                });

        User user = joinRequestDto.toEntity(passwordEncoder.encode(joinRequestDto.getPassword()));
        userRepository.save(user);
    }

    @Override
    public void sendJoinEmail(String email) {
        String code = VerificationCodeUtil.generateEmailVerificationCode();
        final LocalDateTime now = LocalDateTime.now();

        Optional<EmailVerification> optionalEmailVerification = emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(email, EmailPurpose.SIGNUP);
        if (optionalEmailVerification.isPresent()) {
            handleExistingEmailVerification(optionalEmailVerification.get(), code, now);
            return;
        }

        saveNewEmailVerification(email, code, now);
        sendJoinEmailVerificationEmail(email, code);
    }

    private void handleExistingEmailVerification(EmailVerification emailVerification, String code, LocalDateTime now) {
        String email = emailVerification.getEmail();
        if (emailVerification.getStatus() == EmailVerifyStatus.VERIFIED) {
            throw new EmailVerificationAlreadyDoneException();
        } else if (emailVerification.getStatus() == EmailVerifyStatus.LOCKED) {
            handleLockedEmailVerification(emailVerification, code, now);
        } else if (emailVerification.getStatus() == EmailVerifyStatus.PENDING && emailVerification.getExpiresAt().isAfter(now)) {
            handleNotExpiredEmailVerification(emailVerification, code, now);
        } else if (emailVerification.getExpiresAt().isBefore(now)) {
            handleExpiredEmailVerification(emailVerification);
            saveNewEmailVerification(email, code, now);
        }

        emailVerificationRepository.save(emailVerification);
        sendJoinEmailVerificationEmail(email, code);
    }

    private void handleLockedEmailVerification(EmailVerification emailVerification, String code, LocalDateTime now) {
        if (emailVerification.getLockedUntil() != null && now.isBefore(emailVerification.getLockedUntil())) {
            throw new EmailVerificationLockedException();
        }
        emailVerification.updateStatus(EmailVerifyStatus.PENDING);
        emailVerification.updateCode(code);
        emailVerification.resetAttemptCount();
        emailVerification.clearLockedUntil();
    }

    private void handleNotExpiredEmailVerification(EmailVerification emailVerification, String code, LocalDateTime now) {
        emailVerification.updateCode(code);
        emailVerification.resetExpiresAt(now.plusMinutes(expireMinutes));
    }

    private void handleExpiredEmailVerification(EmailVerification emailVerification) {
        emailVerification.updateStatus(EmailVerifyStatus.EXPIRED);
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

    private void sendJoinEmailVerificationEmail(String email, String code) {
        String html = buildVerificationHtml(email, code);
        emailClient.sendOneEmail(email, SUBJECT, html);
    }

    private EmailVerification saveNewEmailVerification(String email, String code, LocalDateTime now) {
        EmailVerification emailVerification = EmailVerification.builder()
                .email(email)
                .purpose(EmailPurpose.SIGNUP)
                .code(code)
                .status(EmailVerifyStatus.PENDING)
                .attemptCount(0)
                .expiresAt(now.plusMinutes(expireMinutes))
                .build();
        return emailVerificationRepository.save(emailVerification);
    }

    // TODO 동시성 테스트 필요
    @Override
    public EmailVerificationConfirmResponseDto verifyEmailCode(EmailVerificationConfirmRequestDto emailVerificationConfirmRequestDto) {
        final LocalDateTime now =  LocalDateTime.now();

        EmailVerification emailVerification = emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(emailVerificationConfirmRequestDto.getEmail(), EmailPurpose.SIGNUP)
                .orElseThrow(() -> new EntityNotFoundException("EmailVerification",  emailVerificationConfirmRequestDto.getEmail()));

        if (emailVerification.getStatus().equals(EmailVerifyStatus.VERIFIED)) {
            log.info("이메일이 이미 인증되었습니다. {}", emailVerificationConfirmRequestDto.getEmail());
            return EmailVerificationConfirmResponseDto.from(emailVerification);
        }

        if (emailVerification.getStatus() == EmailVerifyStatus.LOCKED) {
            LocalDateTime lockUntil = emailVerification.getLockedUntil();

            if (lockUntil != null && now.isBefore(lockUntil)) {
                throw new EmailVerificationLockedException();
            }

            emailVerification.updateStatus(EmailVerifyStatus.PENDING);
            emailVerification.resetAttemptCount();
        }

        if (emailVerification.getExpiresAt().isBefore(now)) {
            emailVerification.updateStatus(EmailVerifyStatus.EXPIRED);
            emailVerificationRepository.save(emailVerification);
            throw new EmailVerificationExpiredException();
        }

        if (emailVerification.getCode().equals(emailVerificationConfirmRequestDto.getCode())) {
            emailVerification.updateStatus(EmailVerifyStatus.VERIFIED);
            emailVerificationRepository.save(emailVerification);
            return EmailVerificationConfirmResponseDto.from(emailVerification);
        }

        emailVerification.increaseAttemptCount();

        if (emailVerification.getAttemptCount() >= maxAttempts) {
            emailVerification.updateStatus(EmailVerifyStatus.LOCKED);
            emailVerification.updateLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
            emailVerificationRepository.save(emailVerification);

            throw new EmailVerificationAttemptLimitExceededException();
        }

        emailVerificationRepository.save(emailVerification);

        return EmailVerificationConfirmResponseDto.from(emailVerification);
    }
}
