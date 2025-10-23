package com.leedahun.storecaseidentity.domain.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import com.leedahun.storecaseidentity.common.response.HttpResponse;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAccessDeniedHandlerTest {

    private final CustomAccessDeniedHandler customAccessDeniedHandler = new CustomAccessDeniedHandler();
    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("AccessDeniedHandler는 401 상태와 JSON 에러 응답을 반환한다")
    void testHandle() throws IOException, ServletException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException accessDeniedException = new AccessDeniedException("access denied");

        // when
        customAccessDeniedHandler.handle(request, response, accessDeniedException);

        // then
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

        String responseBody = response.getContentAsString();
        assertThat(responseBody).isNotBlank();

        HttpResponse httpResponse = om.readValue(responseBody, HttpResponse.class);
        assertThat(httpResponse.getStatus()).isEqualTo(401);
        assertThat(httpResponse.getMessage()).isEqualTo(ErrorMessage.UNAUTHORIZED.getMessage());
        assertThat(httpResponse.getData()).isNull();

    }
}