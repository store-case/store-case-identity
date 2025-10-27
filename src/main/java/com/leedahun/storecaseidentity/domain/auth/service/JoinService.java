package com.leedahun.storecaseidentity.domain.auth.service;

import com.leedahun.storecaseidentity.domain.auth.dto.EmailVerificationConfirmRequestDto;
import com.leedahun.storecaseidentity.domain.auth.dto.EmailVerificationConfirmResponseDto;
import com.leedahun.storecaseidentity.domain.auth.dto.JoinRequestDto;

public interface JoinService {

    void join(JoinRequestDto joinRequestDto);

    void sendJoinEmail(String email);

    EmailVerificationConfirmResponseDto verifyEmailCode(EmailVerificationConfirmRequestDto emailVerificationConfirmRequestDto);

}
