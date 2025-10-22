package com.leedahun.storecaseidentity.domain.auth.filter;

import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import com.leedahun.storecaseidentity.domain.auth.util.AuthenticationResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        AuthenticationResponseUtil.authenticateFail(response, HttpStatus.UNAUTHORIZED, ErrorMessage.UNAUTHORIZED.getMessage());
    }
}
