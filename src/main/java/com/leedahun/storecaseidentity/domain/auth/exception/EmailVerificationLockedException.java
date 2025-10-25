package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class EmailVerificationLockedException extends CustomException {

    public EmailVerificationLockedException() {
        super(ErrorMessage.EMAIL_VERIFICATION_LOCKED.getMessage(), HttpStatus.LOCKED);
    }
}
