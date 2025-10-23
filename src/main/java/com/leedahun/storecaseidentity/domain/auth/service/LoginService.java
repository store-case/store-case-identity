package com.leedahun.storecaseidentity.domain.auth.service;

import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginResponseDto;
import com.leedahun.storecaseidentity.domain.auth.dto.TokenResponseDto;
import com.leedahun.storecaseidentity.domain.auth.entity.Role;

public interface LoginService {

    void join(JoinRequestDto joinRequestDto);

    LoginResponseDto login(LoginRequestDto loginRequestDto);

    TokenResponseDto reissueTokens(String refreshToken);

    TokenResponseDto issueTokens(Long userId, Role role);
}
