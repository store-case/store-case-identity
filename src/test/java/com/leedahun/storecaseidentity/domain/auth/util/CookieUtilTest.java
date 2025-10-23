package com.leedahun.storecaseidentity.domain.auth.util;

import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    @Test
    @DisplayName("refreshToken에서 Bearer 접두사를 제거하고 ResponseCookie를 생성한다")
    void createResponseCookie_shouldReturnHttpOnlySecureCookie_withoutBearerPrefix() {
        // given
        String refreshToken = JwtConstants.TOKEN_PREFIX + "abc.def.ghi";
        long maxAgeMs = 60_000L; // 1분

        // when
        ResponseCookie cookie = CookieUtil.createResponseCookie(refreshToken, maxAgeMs);

        // then
        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("abc.def.ghi");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("None");
        assertThat(cookie.getPath()).isEqualTo("/api/auth/refresh");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMillis(maxAgeMs));
    }

    @Test
    @DisplayName("clearRefreshCookie는 Set-Cookie 헤더를 추가하며, 값이 비어 있고 maxAge=0인 쿠키를 생성한다")
    void clearRefreshCookie_shouldAddExpiredCookieHeader() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        CookieUtil.clearRefreshCookie(response);

        // then
        String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("refreshToken=");
        assertThat(setCookieHeader).contains("Max-Age=0");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Secure");
        assertThat(setCookieHeader).contains("SameSite=None");
    }
}