package com.leedahun.storecaseidentity.domain.auth.constant;

public final class JwtConstants {
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String CLAIM_SUBJECT = "token";
    public static final String CLAIM_ID = "id";
    public static final String CLAIM_ROLE = "role";

    private JwtConstants() {};
}
