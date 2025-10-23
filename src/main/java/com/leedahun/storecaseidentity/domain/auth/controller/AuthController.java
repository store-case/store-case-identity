package com.leedahun.storecaseidentity.domain.auth.controller;

import com.leedahun.storecaseidentity.common.error.exception.EntityNotFoundException;
import com.leedahun.storecaseidentity.common.message.SuccessMessage;
import com.leedahun.storecaseidentity.common.response.HttpResponse;
import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import com.leedahun.storecaseidentity.domain.auth.dto.TokenResponseDto;
import com.leedahun.storecaseidentity.domain.auth.entity.User;
import com.leedahun.storecaseidentity.domain.auth.exception.RefreshTokenNotExistsException;
import com.leedahun.storecaseidentity.domain.auth.repository.UserRepository;
import com.leedahun.storecaseidentity.domain.auth.service.LoginService;
import com.leedahun.storecaseidentity.domain.auth.util.CookieUtil;
import com.leedahun.storecaseidentity.domain.auth.util.PrincipalUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final JwtProperties jwtProperties;

    private final UserRepository userRepository;  // TODO 제거

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody JoinRequestDto joinRequestDto) {
        loginService.join(joinRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new HttpResponse(HttpStatus.CREATED, SuccessMessage.WRITE_SUCCESS.getMessage(), null));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        TokenResponseDto tokens = loginService.login(loginRequestDto);
        ResponseCookie refreshCookie = CookieUtil.createResponseCookie(tokens.getRefreshToken(), jwtProperties.getRefreshExpirationTime());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.LOGIN_SUCCESS.getMessage(), tokens.getAccessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshCookie,
                                     HttpServletResponse response) {
        if (refreshCookie == null || refreshCookie.isBlank()) {
            CookieUtil.clearRefreshCookie(response);
            throw new RefreshTokenNotExistsException();
        }

        TokenResponseDto tokens = loginService.reissueTokens(refreshCookie);
        ResponseCookie newRefreshCookie = CookieUtil.createResponseCookie(tokens.getRefreshToken(), jwtProperties.getRefreshExpirationTime());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie.toString())
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.CREATE_TOKENS.getMessage(), tokens.getAccessToken()));
    }

    // TODO 제거
    @GetMapping("/test")
    public ResponseEntity<?> test(@AuthenticationPrincipal LoginUser loginUser) {
        Long userId = PrincipalUtil.getUserId(loginUser);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", loginUser.getId()));
        return ResponseEntity.ok().body(user);
    }

}
