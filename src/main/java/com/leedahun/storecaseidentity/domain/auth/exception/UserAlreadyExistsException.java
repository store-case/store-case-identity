package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends CustomException {
    public UserAlreadyExistsException() {
        super(ErrorMessage.EMAIL_ALREADY_EXISTS.getMessage(), HttpStatus.CONFLICT);
    }
}
