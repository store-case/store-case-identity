package com.leedahun.storecaseidentity.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResult {
    private LoginResponseDto loginResponseDto;
    private String refreshToken;

    public static LoginResult from(LoginResponseDto loginResponseDto, String refreshToken) {
        return LoginResult.builder()
                .loginResponseDto(loginResponseDto)
                .refreshToken(refreshToken)
                .build();
    }
}
