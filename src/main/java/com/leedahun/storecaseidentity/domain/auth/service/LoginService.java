package com.leedahun.storecaseidentity.domain.auth.service;

import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.TokenResponseDto;

public interface LoginService {

    void join(JoinRequestDto joinRequestDto);

    TokenResponseDto login(LoginRequestDto loginRequestDto);

    TokenResponseDto reissueTokens(String refreshToken);

}
