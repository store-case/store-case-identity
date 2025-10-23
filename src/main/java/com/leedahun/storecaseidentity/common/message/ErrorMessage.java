package com.leedahun.storecaseidentity.common.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    INTERNAL_SERVER_ERROR("서버 에러가 발생했습니다."),
    ENTITY_NOT_FOUND("데이터가 존재하지 않습니다. "),

    EMAIL_ALREADY_EXISTS("이미 존재하는 이메일입니다."),
    TOKEN_EXPIRED("만료된 토큰입니다."),
    INVALID_TOKEN("유효하지 않은 토큰입니다."),
    EMPTY_TOKEN("토큰이 존재하지 않습니다."),
    INVALID_PASSWORD("비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED("인증이 필요합니다."),
    FORBIDDEN("권한이 없습니다.");

    private final String message;
}
