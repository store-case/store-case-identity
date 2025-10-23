package com.leedahun.storecaseidentity.domain.auth.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import com.leedahun.storecaseidentity.domain.auth.exception.InvalidJwtTokenException;
import com.leedahun.storecaseidentity.domain.auth.exception.JwtTokenExpiredException;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long userId, Role role) {
        return createToken(userId, role, jwtProperties.getExpirationTime());
    }

    public String createRefreshToken(Long userId, Role role) {
        return createToken(userId, role, jwtProperties.getRefreshExpirationTime());
    }

    private String createToken(Long id, Role role, long expirationTime) {
        return JWT.create()
                .withSubject(JwtConstants.CLAIM_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .withClaim(JwtConstants.CLAIM_ID, id)
                .withClaim(JwtConstants.CLAIM_ROLE, role.name())
                .sign(Algorithm.HMAC512(jwtProperties.getSecret()));
    }

    public LoginUser verify(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(jwtProperties.getSecret()))
                    .build()
                    .verify(token);

            return LoginUser.builder()
                    .id(decodedJWT.getClaim(JwtConstants.CLAIM_ID).asLong())
                    .role(decodedJWT.getClaim(JwtConstants.CLAIM_ROLE).asString())
                    .build();
        } catch (TokenExpiredException e) {
            throw new JwtTokenExpiredException();
        } catch (JWTVerificationException e) {
            throw new InvalidJwtTokenException();
        }
    }
}