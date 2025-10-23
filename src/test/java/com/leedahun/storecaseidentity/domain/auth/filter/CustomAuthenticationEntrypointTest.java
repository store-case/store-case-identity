package com.leedahun.storecaseidentity.domain.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import com.leedahun.storecaseidentity.common.response.HttpResponse;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAuthenticationEntrypointTest {

    private final CustomAuthenticationEntrypoint entrypoint = new CustomAuthenticationEntrypoint();
    private final ObjectMapper om = new ObjectMapper();

    @Test
    @DisplayName("AuthenticationEntryPoint는 401 상태와 JSON 바디를 작성한다")
    void testCommence() throws IOException, ServletException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/secure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException authEx = new BadCredentialsException("bad credentials");

        // when
        entrypoint.commence(request, response, authEx);

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