package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import org.springframework.http.HttpStatus;

public class JwtTokenExpiredException extends CustomException {

    public JwtTokenExpiredException() {
        super(ErrorMessage.TOKEN_EXPIRED.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
