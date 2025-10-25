package com.leedahun.storecaseidentity.domain.auth.util;

import java.security.SecureRandom;

public class VerificationCodeUtil {

    static final SecureRandom rng = new SecureRandom();

    public static String generateEmailVerificationCode() {
        int n = rng.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}
