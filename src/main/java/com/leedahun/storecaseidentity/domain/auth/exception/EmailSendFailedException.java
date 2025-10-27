package com.leedahun.storecaseidentity.domain.auth.exception;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EmailSendFailedException extends CustomException {

    public EmailSendFailedException() {
        super("메일 전송에 실패하였습니다.", HttpStatus.BAD_GATEWAY);
    }
}
