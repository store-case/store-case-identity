package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class RefreshTokenNotExistsException extends CustomException {

    public RefreshTokenNotExistsException() {
        super(ErrorMessage.EMPTY_TOKEN.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
