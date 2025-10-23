package com.leedahun.storecaseidentity.domain.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leedahun.storecaseidentity.common.response.HttpResponse;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationResponseUtilTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("authenticateFail은 HttpResponse를 JSON으로 변환해 응답 본문에 작성한다")
    void authenticateFail_shouldWriteHttpResponseAsJson() throws IOException, ServletException {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String message = "Invalid token";

        // when
        AuthenticationResponseUtil.authenticateFail(response, status, message);

        // then
        // ContentType 확인
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");

        // 응답 JSON 파싱
        String jsonBody = response.getContentAsString();
        HttpResponse parsedJson = objectMapper.readValue(jsonBody, HttpResponse.class);

        // 응답 구조 검증
        assertThat(parsedJson.getStatus()).isEqualTo(status.value());
        assertThat(parsedJson.getMessage()).isEqualTo(message);
        assertThat(parsedJson.getData()).isNull();
    }
}