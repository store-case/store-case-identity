package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends CustomException {

    public InvalidPasswordException() {
        super(ErrorMessage.INVALID_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
