package com.leedahun.storecaseidentity.domain.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.config.SecurityConfig;
import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.TokenResponseDto;
import com.leedahun.storecaseidentity.domain.auth.exception.RefreshTokenNotExistsException;
import com.leedahun.storecaseidentity.domain.auth.filter.JwtAuthorizationFilter;
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
            type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, JwtAuthorizationFilter.class}
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

        willDoNothing().given(loginService).join(any(JoinRequestDto.class));

        // when & then
        mockMvc.perform(post("/api/auth/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(joinRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("201"))
                .andExpect(jsonPath("$.message").exists());

        then(loginService).should(times(1)).join(any(JoinRequestDto.class));
    }

    @Test
    @DisplayName("[POST /api/auth/login] 로그인 성공 시 Set-Cookie(refreshToken), 액세스 토큰을 반환한다")
    void login_success() throws Exception {
        // given
        LoginRequestDto loginRequest = new LoginRequestDto("a@a.com", "pw");
        TokenResponseDto tokens = TokenResponseDto.builder()
                .accessToken(JwtConstants.TOKEN_PREFIX + "access.raw")
                .refreshToken(JwtConstants.TOKEN_PREFIX + "refresh.raw")
                .build();

        given(loginService.login(any(LoginRequestDto.class))).willReturn(tokens);
        given(jwtProperties.getRefreshExpirationTime()).willReturn(REFRESH_EXPIRATION_TIME);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loginRequest)))
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

        then(loginService).should(times(1)).login(any(LoginRequestDto.class));
    }

    @Test
    @DisplayName("[POST /api/auth/refresh] 쿠키에 담긴 refreshToken으로 재발급 성공 시 새 Set-Cookie와 새 accessToken을 반환한다")
    void refresh_success() throws Exception {
        // given
        String cookieValue = "refresh.raw";
        TokenResponseDto tokens = TokenResponseDto.builder()
                .accessToken(JwtConstants.TOKEN_PREFIX + "new.access.raw")
                .refreshToken(JwtConstants.TOKEN_PREFIX + "new.refresh.raw")
                .build();

        given(loginService.reissueTokens(cookieValue)).willReturn(tokens);
        given(jwtProperties.getRefreshExpirationTime()).willReturn(REFRESH_EXPIRATION_TIME);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
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
        MvcResult result = mockMvc.perform(post("/api/auth/refresh"))
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

}