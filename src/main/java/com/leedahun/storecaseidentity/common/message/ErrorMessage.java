package com.leedahun.storecaseidentity.common.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorMessage {
    INTERNAL_SERVER_ERROR("서버 에러가 발생했습니다."),
    ENTITY_NOT_FOUND("데이터가 존재하지 않습니다. ");

    private final String message;
}
