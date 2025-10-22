package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class InvalidJwtTokenException extends CustomException {

    public InvalidJwtTokenException() {
        super(ErrorMessage.INVALID_TOKEN.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
