package com.leedahun.storecaseidentity.domain.auth.controller;

import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import com.leedahun.storecaseidentity.common.message.SuccessMessage;
import com.leedahun.storecaseidentity.common.response.HttpResponse;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.dto.*;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerifyStatus;
import com.leedahun.storecaseidentity.domain.auth.exception.RefreshTokenNotExistsException;
import com.leedahun.storecaseidentity.domain.auth.service.JoinService;
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
@RequestMapping("/api/identity/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final JoinService joinService;
    private final JwtProperties jwtProperties;

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody JoinRequestDto joinRequestDto) {
        joinService.join(joinRequestDto);
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

    @PostMapping("/join/email")
    public ResponseEntity<?> sendEmailVerification(@RequestBody EmailVerificationSendRequestDto emailVerificationSendRequestDto) {
        joinService.sendJoinEmail(emailVerificationSendRequestDto.getEmail());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new HttpResponse(HttpStatus.CREATED, SuccessMessage.WRITE_SUCCESS.getMessage(), null));
    }

    @PostMapping("/join/email/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerificationConfirmRequestDto emailVerificationConfirmRequestDto) {
        EmailVerificationConfirmResponseDto emailVerificationResult = joinService.verifyEmailCode(emailVerificationConfirmRequestDto);

        String message;
        if (emailVerificationResult.getStatus() == EmailVerifyStatus.VERIFIED) {
            message = SuccessMessage.EMAIL_VERIFIED.getMessage();
        } else {
            message = ErrorMessage.EMAIL_VERIFICATION_FAILED.getMessage();
        }
        return ResponseEntity
                .ok()
                .body(new HttpResponse(HttpStatus.OK, message, emailVerificationResult));
    }

}
