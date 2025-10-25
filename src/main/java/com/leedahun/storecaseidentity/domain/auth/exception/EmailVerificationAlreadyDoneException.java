package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EmailVerificationAlreadyDoneException extends CustomException {

    public EmailVerificationAlreadyDoneException() {
        super("이미 인증된 이메일입니다.", HttpStatus.CONFLICT);
    }
}
