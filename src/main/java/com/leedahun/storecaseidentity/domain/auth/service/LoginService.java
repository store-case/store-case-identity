package com.leedahun.storecaseidentity.domain.auth.service;

import com.leedahun.storecaseidentity.domain.auth.dto.*;

public interface LoginService {

    LoginResult login(LoginRequestDto loginRequestDto);

    TokenResult reissueTokens(String refreshToken);

}
