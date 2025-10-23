package com.leedahun.storecaseidentity.domain.auth.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import com.leedahun.storecaseidentity.domain.auth.exception.InvalidJwtTokenException;
import com.leedahun.storecaseidentity.domain.auth.exception.JwtTokenExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    private final String SECRET = "test-secret-key";
    private final long ACCESS_EXP_MS = 60_000L;
    private final long REFRESH_EXP_MS = 86_400_000L;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        jwtProperties.setExpirationTime(ACCESS_EXP_MS);
        jwtProperties.setRefreshExpirationTime(REFRESH_EXP_MS);

        jwtUtil = new JwtUtil(jwtProperties);
    }

    @Test
    @DisplayName("Access Token 생성 및 검증 성공")
    void createAccessToken_and_verify_success() {
        // given
        Long userId = 100L;
        Role role = Role.USER;

        // when
        String accessToken = jwtUtil.createAccessToken(userId, role);

        // then
        assertThat(accessToken).startsWith(JwtConstants.TOKEN_PREFIX);
        LoginUser loginUser = jwtUtil.verify(accessToken.replaceFirst(JwtConstants.TOKEN_PREFIX, ""));
        assertThat(loginUser.getId()).isEqualTo(userId);
        assertThat(loginUser.getRole()).isEqualTo(role.name());
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증 성공")
    void createRefreshToken_and_verify_success() {
        // given
        Long userId = 7L;
        Role role = Role.ADMIN;

        // when
        String refreshToken = jwtUtil.createRefreshToken(userId, role);

        // then
        assertThat(refreshToken).startsWith(JwtConstants.TOKEN_PREFIX);
        LoginUser loginUser = jwtUtil.verify(refreshToken.replaceFirst(JwtConstants.TOKEN_PREFIX, ""));
        assertThat(loginUser.getId()).isEqualTo(userId);
        assertThat(loginUser.getRole()).isEqualTo(role.name());
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 JwtTokenExpiredException 발생")
    void verify_expiredToken_throwsJwtTokenExpiredException() {
        // given
        String expiredToken = JWT.create()
                .withSubject(JwtConstants.CLAIM_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() - 1000))
                .withClaim(JwtConstants.CLAIM_ID, 1L)
                .withClaim(JwtConstants.CLAIM_ROLE, Role.USER.name())
                .sign(Algorithm.HMAC512(SECRET));

        // then
        assertThrows(JwtTokenExpiredException.class, () -> jwtUtil.verify(expiredToken));
    }

    @Test
    @DisplayName("서명이 잘못된 토큰은 InvalidJwtTokenException 발생")
    void verify_invalidSignature_throwsInvalidJwtTokenException() {
        // given
        String invalidToken = JWT.create()
                .withSubject(JwtConstants.CLAIM_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .withClaim(JwtConstants.CLAIM_ID, 2L)
                .withClaim(JwtConstants.CLAIM_ROLE, Role.USER.name())
                .sign(Algorithm.HMAC512("DIFFERENT_SECRET"));

        // then
        assertThrows(InvalidJwtTokenException.class, () -> jwtUtil.verify(invalidToken));
    }

    @Test
    @DisplayName("접두사가 포함된 토큰을 그대로 검증하면 실패해야 한다")
    void verify_withPrefix_shouldFail() {
        // given
        String accessToken = jwtUtil.createAccessToken(3L, Role.USER);

        // then
        assertThrows(InvalidJwtTokenException.class, () -> jwtUtil.verify(accessToken));
    }
}