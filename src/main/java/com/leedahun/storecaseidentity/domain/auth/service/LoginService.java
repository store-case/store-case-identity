package com.leedahun.storecaseidentity.domain.auth.service;

import com.leedahun.storecaseidentity.domain.auth.dto.*;

public interface LoginService {

    void join(JoinRequestDto joinRequestDto);

    LoginResult login(LoginRequestDto loginRequestDto);

    TokenResult reissueTokens(String refreshToken);

}
