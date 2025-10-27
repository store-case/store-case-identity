package com.leedahun.storecaseidentity.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import com.leedahun.storecaseidentity.common.message.SuccessMessage;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.config.SecurityConfig;
import com.leedahun.storecaseidentity.domain.auth.dto.*;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerifyStatus;
import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import com.leedahun.storecaseidentity.domain.auth.exception.RefreshTokenNotExistsException;
import com.leedahun.storecaseidentity.domain.auth.service.JoinService;
import com.leedahun.storecaseidentity.domain.auth.service.LoginService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class}
))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    LoginService loginService;

    @MockitoBean
    JoinService joinService;

    @MockitoBean
    JwtProperties jwtProperties;

    private static final long REFRESH_EXPIRATION_TIME = 86_400_000L;

    @Test
    @DisplayName("[POST /api/auth/join] 회원가입 성공 시 201 CREATED와 본문을 반환한다")
    void join_success() throws Exception {
        // given
        JoinRequestDto joinRequest = JoinRequestDto.builder()
                .email("test@gmail.com")
                .name("user")
                .password("12345678")
                .phone("010-1234-1234")
                .build();

        willDoNothing().given(joinService).join(any(JoinRequestDto.class));

        // when & then
        mockMvc.perform(post("/api/identity/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.message").exists());

        then(joinService).should(times(1)).join(any(JoinRequestDto.class));
    }

    @Test
    @DisplayName("[POST /api/auth/login] 로그인 성공 시 Set-Cookie(refreshToken), 액세스 토큰을 반환한다")
    void login_success() throws Exception {
        // given
        LoginRequestDto loginRequest = new LoginRequestDto("a@a.com", "pw");
        LoginResponseDto loginResponseDto = LoginResponseDto.builder()
                .id(1L)
                .email("test@email.com")
                .name("user")
                .role(Role.USER)
                .accessToken("access.raw")
                .build();
        LoginResult loginResult = LoginResult.builder()
                .loginResponseDto(loginResponseDto)
                .refreshToken("refresh.raw")
                .build();

        given(loginService.login(any(LoginRequestDto.class))).willReturn(loginResult);
        given(jwtProperties.getRefreshExpirationTime()).willReturn(REFRESH_EXPIRATION_TIME);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/identity/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("test@email.com"))
                .andExpect(jsonPath("$.data.name").value("user"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.accessToken").value(loginResponseDto.getAccessToken()))
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=None");
        assertThat(setCookie).contains("Path=/api/auth/refresh");

        then(loginService).should(times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("[POST /api/auth/refresh] 쿠키에 담긴 refreshToken으로 재발급 성공 시 새 Set-Cookie와 새 accessToken을 반환한다")
    void refresh_success() throws Exception {
        // given
        String cookieValue = "refresh.raw";
        TokenResult tokens = TokenResult.builder()
                .accessToken("new.access.raw")
                .refreshToken("new.refresh.raw")
                .build();

        given(loginService.reissueTokens(cookieValue)).willReturn(tokens);
        given(jwtProperties.getRefreshExpirationTime()).willReturn(REFRESH_EXPIRATION_TIME);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/identity/auth/refresh")
                        .cookie(new MockCookie("refreshToken", cookieValue)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.data").value(tokens.getAccessToken()))
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=None");
        assertThat(setCookie).contains("Path=/api/auth/refresh");

        then(loginService).should(times(1)).reissueTokens(cookieValue);
    }

    @Test
    @DisplayName("[POST /api/auth/refresh] refreshToken 쿠키 없을 경우 쿠키 제거(Set-Cookie: Max-Age=0) 후 예외를 발생한다")
    void refresh_withoutCookie_throws_and_clears() throws Exception {
        // when
        MvcResult result = mockMvc.perform(post("/api/identity/auth/refresh"))
                .andExpect(resultMatcher -> {
                    Throwable ex = resultMatcher.getResolvedException();
                    assertThat(ex).isInstanceOf(RefreshTokenNotExistsException.class);
                })
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("refreshToken=");
        assertThat(setCookie).contains("Max-Age=0");

        then(loginService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("[POST /api/identity/auth/join/email] 인증 메일 전송 성공 시 201 CREATED와 본문을 반환한다")
    void send_email_verification_success() throws Exception {
        // given
        EmailVerificationSendRequestDto emailVerificationSendRequest = new EmailVerificationSendRequestDto("user@test.com");
        willDoNothing().given(joinService).sendJoinEmail(eq("user@test.com"));

        // when & then
        mockMvc.perform(post("/api/identity/auth/join/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(emailVerificationSendRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.message").value(SuccessMessage.WRITE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());

        then(joinService).should(times(1)).sendJoinEmail(eq("user@test.com"));
    }

    @Test
    @DisplayName("[POST /api/identity/auth/join/email/verify] 코드 일치(VERIFIED) 시 200 OK와 '인증 성공' 메시지 및 상태를 반환한다")
    void verify_email_code_verified_success() throws Exception {
        // given
        EmailVerificationConfirmRequestDto emailVerificationConfirmRequest =
                new EmailVerificationConfirmRequestDto("user@test.com", "123456");

        EmailVerificationConfirmResponseDto emailVerificationConfirmResult =
                EmailVerificationConfirmResponseDto.builder()
                        .status(EmailVerifyStatus.VERIFIED)
                        .attempts(1)
                        .build();

        given(joinService.verifyEmailCode(any(EmailVerificationConfirmRequestDto.class)))
                .willReturn(emailVerificationConfirmResult);

        // when & then
        mockMvc.perform(post("/api/identity/auth/join/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(emailVerificationConfirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value(SuccessMessage.EMAIL_VERIFIED.getMessage()))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.attempts").value(1));

        then(joinService).should(times(1)).verifyEmailCode(any(EmailVerificationConfirmRequestDto.class));
    }

    @Test
    @DisplayName("[POST /api/identity/auth/join/email/verify] 미인증 상태(PENDING 등) 시 200 OK와 '인증 실패' 메시지 및 상태를 반환한다")
    void verify_email_code_not_verified_returns_failed_message() throws Exception {
        // given
        EmailVerificationConfirmRequestDto emailVerificationConfirmRequest =
                new EmailVerificationConfirmRequestDto("user@test.com", "000000");

        EmailVerificationConfirmResponseDto emailVerificationConfirmResult =
                EmailVerificationConfirmResponseDto.builder()
                        .status(EmailVerifyStatus.PENDING)
                        .attempts(2)
                        .build();

        given(joinService.verifyEmailCode(any(EmailVerificationConfirmRequestDto.class)))
                .willReturn(emailVerificationConfirmResult);

        // when & then
        mockMvc.perform(post("/api/identity/auth/join/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(emailVerificationConfirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("200"))
                .andExpect(jsonPath("$.message").value(ErrorMessage.EMAIL_VERIFICATION_FAILED.getMessage()))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.attempts").value(2));

        then(joinService).should(times(1)).verifyEmailCode(any(EmailVerificationConfirmRequestDto.class));
    }

}