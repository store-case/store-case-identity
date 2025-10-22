package com.leedahun.storecaseidentity.domain.auth.util;

import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;

public class PrincipalUtil {

    public static Long getUserId(LoginUser loginUser) {
        if (loginUser == null) {
            return null;
        }

        return loginUser.getId();
    }
}
