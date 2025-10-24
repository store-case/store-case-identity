package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailVerificationExpiredException extends CustomException {

    public EmailVerificationExpiredException() {
        super(ErrorMessage.EMAIL_VERIFICATION_EXPIRED.getMessage(), HttpStatus.GONE);
    }
}
