package com.leedahun.storecaseidentity.domain.auth.util;

import com.leedahun.storecaseidentity.domain.auth.dto.JwtConstants;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static ResponseCookie createResponseCookie(String refreshToken, Long maxAge) {
        refreshToken = refreshToken.replace(JwtConstants.TOKEN_PREFIX, "");
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth/refresh")
                .maxAge(Duration.ofMillis(maxAge))
                .build();
    }

    public static void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie refreshCookie = createResponseCookie("", 0L);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }
}
