package com.leedahun.storecaseidentity.domain.auth.util;

import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrincipalUtilTest {

    @Test
    @DisplayName("LoginUser가 null이면 null을 반환한다")
    void getUserId_nullLoginUser_returnsNull() {
        // given
        LoginUser loginUser = null;

        // when
        Long userId = PrincipalUtil.getUserId(loginUser);

        // then
        assertThat(userId).isNull();
    }

    @Test
    @DisplayName("LoginUser의 id를 정상적으로 반환한다")
    void getUserId_validLoginUser_returnsId() {
        // given
        LoginUser loginUser = LoginUser.builder()
                .id(42L)
                .role("USER")
                .build();

        // when
        Long userId = PrincipalUtil.getUserId(loginUser);

        // then
        assertThat(userId).isEqualTo(42L);
    }
}