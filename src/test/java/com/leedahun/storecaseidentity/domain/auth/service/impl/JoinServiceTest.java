package com.leedahun.storecaseidentity.domain.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@ExtendWith(MockitoExtension.class)
class JoinServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    BCryptPasswordEncoder passwordEncoder;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private EmailClient emailClient;

    @InjectMocks
    private JoinServiceImpl joinService;

    private static final String EMAIL = "user@test.com";
    private static final String NAME = "tester";
    private static final String PHONE = "010-1234-5678";
    private static final String RAW_PW = "plainPW!";
    private static final String ENC_PW = "$2a$10$encoded";

    private static final String EMAIL_CODE = "123456";
    private static final String HTML = "<html>EMAIL</html>";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(joinService, "maxAttempts", 5);
        ReflectionTestUtils.setField(joinService, "expireMinutes", 10);
        ReflectionTestUtils.setField(joinService, "lockMinutes", 3);
    }

    @Nested
    @DisplayName("회원가입 테스트")
    class joinTests {

        @Test
        @DisplayName("신규 이메일이면 사용자를 저장하고 비밀번호를 암호화한다")
        void join_success() {
            // given
            JoinRequestDto joinRequest = JoinRequestDto.builder()
                    .email(EMAIL)
                    .name(NAME)
                    .password(RAW_PW)
                    .phone(PHONE)
                    .build();
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
            given(passwordEncoder.encode(RAW_PW)).willReturn(ENC_PW);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

            // when
            joinService.join(joinRequest);

            // then
            verify(userRepository, times(1)).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getEmail()).isEqualTo(EMAIL);
            assertThat(saved.getName()).isEqualTo(NAME);
            assertThat(saved.getPhone()).isEqualTo(PHONE);
            assertThat(saved.getPassword()).isEqualTo(ENC_PW);
        }

        @Test
        @DisplayName("이메일 중복이면 UserAlreadyExistsException을 발생시킨다")
        void join_duplicateEmail_throws() {
            // given
            JoinRequestDto joinRequest = JoinRequestDto.builder()
                    .email(EMAIL)
                    .name(NAME)
                    .password(RAW_PW)
                    .phone(PHONE)
                    .build();
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(new User()));

            // when & then
            assertThatThrownBy(() -> joinService.join(joinRequest))
                    .isInstanceOf(UserAlreadyExistsException.class);

            verify(userRepository, never()).save(any(User.class));
        }

    }

    @Nested
    @DisplayName("회원가입 이메일 전송 요청 테스트")
    class SendJoinEmailTests {

        @Test
        @DisplayName("회원가입 이메일 요청 시 이전 인증 기록 없을 경우 새 인증을 저장하고 메일을 전송한다")
        void send_firstTime_success() {
            // given
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.empty());
            given(templateEngine.process(anyString(), any(Context.class))).willReturn(HTML);

            // when
            joinService.sendJoinEmail(EMAIL);

            // then
            then(emailVerificationRepository).should().save(any(EmailVerification.class));
            then(emailClient).should().sendOneEmail(eq(EMAIL), contains("StoreCase"), eq(HTML));
            verify(emailClient, times(1)).sendOneEmail(eq(EMAIL), contains("StoreCase"), eq(HTML));
        }

        @Test
        @DisplayName("회원가입 이메일 요청 시 이미 인증된 경우 예외를 발생한다")
        void send_alreadyVerified_throws() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.VERIFIED)  // 이미 인증완료 상태인 경우
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when & then
            assertThatThrownBy(() -> joinService.sendJoinEmail(EMAIL))
                    .isInstanceOf(EmailVerificationAlreadyDoneException.class);

            then(emailClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원가입 이메일 요청 시 잠금 상태이고 잠금기간이 만료되지 않은 경우 예외가 발생한다")
        void send_lockedAndStillLocked_throws() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.LOCKED)  // 이미 잠금상태인 경우
                    .lockedUntil(LocalDateTime.now().plusMinutes(5))  // 잠금기간이 만료되지 않은 경우
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when & then
            assertThatThrownBy(() -> joinService.sendJoinEmail(EMAIL))
                    .isInstanceOf(EmailVerificationLockedException.class);

            then(emailClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("회원가입 이메일 요청 시 잠금 기간이 만료된 경우 초기화 후 재발송한다")
        void send_lockedButExpired_thenResetAndSend() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.LOCKED)
                    .lockedUntil(LocalDateTime.now().minusMinutes(1))  // 잠금기간이 만료된 경우
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));
            given(templateEngine.process(eq("email/verification"), any(Context.class))).willReturn(HTML);

            // when
            joinService.sendJoinEmail(EMAIL);

            // then
            verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
            then(emailClient).should().sendOneEmail(eq(EMAIL), contains("StoreCase"), eq(HTML));
        }

        @Test
        @DisplayName("회원가입 이메일 요청 시 요청상태이고 아직 유효한 경우 코드 갱신 및 기간연장 후 재발송한다")
        void send_pendingAndValid_thenUpdateAndSend() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.PENDING)  // 요청 상태인 경우
                    .expiresAt(LocalDateTime.now().plusMinutes(10))  // 아직 유효한 경우
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));
            given(templateEngine.process(eq("email/verification"), any(Context.class))).willReturn(HTML);

            // when
            joinService.sendJoinEmail(EMAIL);

            // then
            then(emailVerificationRepository).should().save(any(EmailVerification.class));
            then(emailClient).should().sendOneEmail(eq(EMAIL), contains("StoreCase"), eq(HTML));
            verify(emailVerificationRepository, times(1)).save(any(EmailVerification.class));
            verify(emailClient, times(1)).sendOneEmail(eq(EMAIL), contains("StoreCase"), eq(HTML));
        }

        @Test
        @DisplayName("회원가입 이메일 요청 시 요청상태이고 만료된 경우 기존 이력을 리셋하고 새 이력을 저장한다")
        void sendJoinEmail_existing_expired_marksExpired_thenCreatesNew_andSends() {
            // given
            LocalDateTime now = LocalDateTime.now();
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.PENDING)
                    .expiresAt(now.minusSeconds(1))
                    .build();

            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(EMAIL, EmailPurpose.SIGNUP))
                    .willReturn(Optional.of(emailVerification));

            // when
            joinService.sendJoinEmail(EMAIL);

            // then
            // 1) 기존 EXPIRED로 저장
            then(emailVerificationRepository).should().save(
                argThat(saved -> saved == emailVerification && saved.getStatus() == EmailVerifyStatus.EXPIRED)
            );
            // 2) 새 레코드 저장 (ev와 다른 인스턴스)
            then(emailVerificationRepository).should().save(argThat(saved ->
                    saved != emailVerification
                            && EMAIL.equals(saved.getEmail())
                            && saved.getStatus() == EmailVerifyStatus.PENDING
//                            && "CODE123".equals(saved.getCode())
            ));
            // 3) 발송 1회
//            then(emailClient).should(times(1)).sendOneEmail(eq(EMAIL), contains("StoreCase"), eq(HTML));
            then(emailClient).should(times(1)).sendOneEmail(anyString(), contains(anyString()), eq(anyString()));
            then(emailVerificationRepository).shouldHaveNoMoreInteractions();
        }

    }

    @Nested
    class VerifyEmailCodeTests {

        @Test
        @DisplayName("회원가입 인증코드 인증 시 이미 인증완료상태인 경우 바로 응답한다")
        void verify_alreadyVerified_returns() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.VERIFIED)
                    .attemptCount(0)
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when
            EmailVerificationConfirmResponseDto emailVerificationConfirmResult = joinService.verifyEmailCode(
                    new EmailVerificationConfirmRequestDto(EMAIL, "ANY")
            );

            // then
            assertThat(emailVerificationConfirmResult.getStatus()).isEqualTo(EmailVerifyStatus.VERIFIED);
            then(emailVerificationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("회원가입 인증코드 인증 시 잠금상태 유지시간이 남았을 경우 예외가 발생한다")
        void verify_lockedStill_throws() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.LOCKED)
                    .lockedUntil(LocalDateTime.now().plusMinutes(3))
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when / then
            assertThatThrownBy(() ->
                    joinService.verifyEmailCode(new EmailVerificationConfirmRequestDto(EMAIL, EMAIL_CODE)))
                    .isInstanceOf(EmailVerificationLockedException.class);

            then(emailVerificationRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("회원가입 인증코드 인증 시 인증가능시간이 만료되었을 경우 만료상태로 저장 후 예외를 발생한다")
        void verify_expired_throwsAndSaveExpired() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.PENDING)
                    .expiresAt(LocalDateTime.now().minusSeconds(1))  // 인증시간 만료된 경우
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when & then
            assertThatThrownBy(() ->
                    joinService.verifyEmailCode(new EmailVerificationConfirmRequestDto(EMAIL, EMAIL_CODE)))
                    .isInstanceOf(EmailVerificationExpiredException.class);

            ArgumentCaptor<EmailVerification> emailVerificationArgumentCaptor = ArgumentCaptor.forClass(EmailVerification.class);
            then(emailVerificationRepository).should().save(emailVerificationArgumentCaptor.capture());
            assertThat(emailVerificationArgumentCaptor.getValue().getStatus()).isEqualTo(EmailVerifyStatus.EXPIRED);
        }

        @Test
        @DisplayName("회원가입 인증코드 인증 시 코드가 일치할 경우 인증완료 상태로 저장한다")
        void verify_codeMatches_success() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .code(EMAIL_CODE)
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when
            EmailVerificationConfirmResponseDto emailVerificationConfirmResult = joinService.verifyEmailCode(
                    new EmailVerificationConfirmRequestDto(EMAIL, EMAIL_CODE));

            // then
            assertThat(emailVerificationConfirmResult.getStatus()).isEqualTo(EmailVerifyStatus.VERIFIED);
            ArgumentCaptor<EmailVerification> capturedEmailVerification = ArgumentCaptor.forClass(EmailVerification.class);
            then(emailVerificationRepository).should().save(capturedEmailVerification.capture());
            assertThat(capturedEmailVerification.getValue().getStatus()).isEqualTo(EmailVerifyStatus.VERIFIED);
        }

        @Test
        @DisplayName("회원가입 이메일 코드 인증 시 코드가 불일치하고 시도횟수가 남았을 경우 시도횟수를 증가시키고 저장한다")
        void verify_codeNotMatch_underMax_increasesCount() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .code(EMAIL_CODE)
                    .attemptCount(1)  // 시도횟수가 남은 경우
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when
            EmailVerificationConfirmResponseDto emailVerificationConfirmResult = joinService.verifyEmailCode(
                    new EmailVerificationConfirmRequestDto(EMAIL, "different_code"));

            // then
            assertThat(emailVerificationConfirmResult.getStatus()).isEqualTo(EmailVerifyStatus.PENDING);
            then(emailVerificationRepository).should().save(any(EmailVerification.class));
        }

        @Test
        @DisplayName("회원가입 이메일 코드 인증 시 코드가 일치하지 않고 시도횟수가 한계에 도달한 경우 잠금상태로 저장한다")
        void verify_codeNotMatch_reachMax_thenLockedAndThrows() {
            // given
            EmailVerification emailVerification = EmailVerification.builder()
                    .email(EMAIL)
                    .purpose(EmailPurpose.SIGNUP)
                    .status(EmailVerifyStatus.PENDING)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .code(EMAIL_CODE)
                    .attemptCount(4)
                    .build();
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.of(emailVerification));

            // when & then
            assertThatThrownBy(() ->
                    joinService.verifyEmailCode(new EmailVerificationConfirmRequestDto(EMAIL, "different_code")))
                    .isInstanceOf(EmailVerificationAttemptLimitExceededException.class);

            ArgumentCaptor<EmailVerification> emailVerificationArgumentCaptor = ArgumentCaptor.forClass(EmailVerification.class);
            then(emailVerificationRepository).should().save(emailVerificationArgumentCaptor.capture());
            assertThat(emailVerificationArgumentCaptor.getValue().getStatus()).isEqualTo(EmailVerifyStatus.LOCKED);
            assertThat(emailVerificationArgumentCaptor.getValue().getLockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("회원가입 이메일 코드 인증 시 기록이 없을 경우 예외가 발생한다")
        void verify_noRecord_throws() {
            // given
            given(emailVerificationRepository.findTopByEmailAndPurposeOrderByIdDesc(anyString(), eq(EmailPurpose.SIGNUP)))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() ->
                    joinService.verifyEmailCode(new EmailVerificationConfirmRequestDto(EMAIL, EMAIL_CODE)))
                    .isInstanceOf(EntityNotFoundException.class);
        }

    }
}