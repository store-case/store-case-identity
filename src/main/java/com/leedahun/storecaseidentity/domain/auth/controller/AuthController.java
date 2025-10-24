package com.leedahun.storecaseidentity.domain.auth.controller;

import com.leedahun.storecaseidentity.common.message.SuccessMessage;
import com.leedahun.storecaseidentity.common.response.HttpResponse;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.dto.*;
import com.leedahun.storecaseidentity.domain.auth.exception.RefreshTokenNotExistsException;
import com.leedahun.storecaseidentity.domain.auth.service.LoginService;
import com.leedahun.storecaseidentity.domain.auth.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final JwtProperties jwtProperties;

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody JoinRequestDto joinRequestDto) {
        loginService.join(joinRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new HttpResponse(HttpStatus.CREATED, SuccessMessage.WRITE_SUCCESS.getMessage(), null));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        LoginResult loginResult = loginService.login(loginRequestDto);
        ResponseCookie refreshCookie = CookieUtil.createResponseCookie(loginResult.getRefreshToken(), jwtProperties.getRefreshExpirationTime());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.LOGIN_SUCCESS.getMessage(), loginResult.getLoginResponseDto()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshCookie,
                                     HttpServletResponse response) {
        if (refreshCookie == null || refreshCookie.isBlank()) {
            CookieUtil.clearRefreshCookie(response);
            throw new RefreshTokenNotExistsException();
        }

        TokenResult tokens = loginService.reissueTokens(refreshCookie);
        ResponseCookie newRefreshCookie = CookieUtil.createResponseCookie(tokens.getRefreshToken(), jwtProperties.getRefreshExpirationTime());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.CREATE_TOKENS.getMessage(), tokens.getAccessToken()));
    }

}
