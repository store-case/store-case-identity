package com.leedahun.storecaseidentity.domain.auth.service.impl;

import com.leedahun.storecaseidentity.common.error.exception.EntityNotFoundException;
import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import com.leedahun.storecaseidentity.domain.auth.dto.TokenResponseDto;
import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import com.leedahun.storecaseidentity.domain.auth.entity.User;
import com.leedahun.storecaseidentity.domain.auth.exception.InvalidPasswordException;
import com.leedahun.storecaseidentity.domain.auth.exception.UserAlreadyExistsException;
import com.leedahun.storecaseidentity.domain.auth.repository.UserRepository;
import com.leedahun.storecaseidentity.domain.auth.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    JwtUtil jwtUtil;

    @Mock
    BCryptPasswordEncoder passwordEncoder;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    LoginServiceImpl loginService;

    private static final String EMAIL = "user@test.com";
    private static final String NAME = "tester";
    private static final String PHONE = "010-1234-5678";
    private static final String RAW_PW = "plainPW!";
    private static final String ENC_PW = "$2a$10$encoded"; // 더미

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
        loginService.join(joinRequest);

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

        // when / then
        assertThatThrownBy(() -> loginService.join(joinRequest))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사용자 정보가 일치하면 토큰을 반환한다")
    void login_success() {
        // given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .password(ENC_PW)
                .name(NAME)
                .phone(PHONE)
                .role(Role.USER)
                .build();

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PW, ENC_PW)).willReturn(true);

        given(jwtUtil.createAccessToken(1L, Role.USER)).willReturn("access.raw");
        given(jwtUtil.createRefreshToken(1L, Role.USER)).willReturn("refresh.raw");

        LoginRequestDto loginRequest = new LoginRequestDto(EMAIL, RAW_PW);

        // when
        TokenResponseDto tokens = loginService.login(loginRequest);

        // then
        assertThat(tokens.getAccessToken()).isEqualTo(JwtConstants.TOKEN_PREFIX + "access.raw");
        assertThat(tokens.getRefreshToken()).isEqualTo(JwtConstants.TOKEN_PREFIX + "refresh.raw");

        verify(userRepository).findByEmail(EMAIL);
        verify(passwordEncoder).matches(RAW_PW, ENC_PW);
        verify(jwtUtil).createAccessToken(1L, Role.USER);
        verify(jwtUtil).createRefreshToken(1L, Role.USER);
    }

    @Test
    @DisplayName("로그인을 시도한 사용자 정보가 없으면 EntityNotFoundException이 발생한다")
    void login_userNotFound_throws() {
        // given
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> loginService.login(new LoginRequestDto(EMAIL, RAW_PW)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).createAccessToken(anyLong(), any());
    }

    @Test
    @DisplayName("비밀번호 불일치면 InvalidPasswordException이 발생한다")
    void login_wrongPassword_throws() {
        // given
        User user = User.builder()
                .id(1L)
                .email(EMAIL)
                .password(ENC_PW)
                .role(Role.USER)
                .build();

        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.matches(RAW_PW, ENC_PW)).willReturn(false);

        // when / then
        assertThatThrownBy(() -> loginService.login(new LoginRequestDto(EMAIL, RAW_PW)))
                .isInstanceOf(InvalidPasswordException.class);

        verify(jwtUtil, never()).createAccessToken(anyLong(), any());
    }

    @Test
    @DisplayName("유효한 refresh 토큰이면 새 토큰을 반환한다")
    void reissueTokens_success() {
        // given
        LoginUser principal = LoginUser.builder()
                .id(10L)
                .role(Role.USER.name())
                .build();
        given(jwtUtil.verify("refresh.raw")).willReturn(principal);

        User user = User.builder()
                .id(10L)
                .email(EMAIL)
                .password(ENC_PW)
                .role(Role.USER)
                .build();
        given(userRepository.findById(10L)).willReturn(Optional.of(user));

        given(jwtUtil.createAccessToken(10L, Role.USER)).willReturn("new.access.raw");
        given(jwtUtil.createRefreshToken(10L, Role.USER)).willReturn("new.refresh.raw");

        // when
        TokenResponseDto tokens = loginService.reissueTokens("refresh.raw");

        // then
        assertThat(tokens.getAccessToken()).isEqualTo(JwtConstants.TOKEN_PREFIX + "new.access.raw");
        assertThat(tokens.getRefreshToken()).isEqualTo(JwtConstants.TOKEN_PREFIX + "new.refresh.raw");

        verify(jwtUtil).verify("refresh.raw");
        verify(userRepository).findById(10L);
        verify(jwtUtil).createAccessToken(10L, Role.USER);
        verify(jwtUtil).createRefreshToken(10L, Role.USER);
    }

    @Test
    @DisplayName("토큰의 사용자 ID가 DB에 없으면 EntityNotFoundException이 발생한다")
    void reissueTokens_userMissing_throws() {
        // given
        LoginUser principal = LoginUser.builder()
                .id(99L)
                .role(Role.USER.name())
                .build();
        given(jwtUtil.verify("refresh.raw")).willReturn(principal);
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> loginService.reissueTokens("refresh.raw"))
                .isInstanceOf(EntityNotFoundException.class);

        verify(jwtUtil, never()).createAccessToken(anyLong(), any());
    }
}