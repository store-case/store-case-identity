package com.leedahun.storecaseidentity.domain.auth.filter;

import com.leedahun.storecaseidentity.common.message.ErrorMessage;
import com.leedahun.storecaseidentity.domain.auth.util.AuthenticationResponseUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntrypoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        AuthenticationResponseUtil.authenticateFail(response, HttpStatus.UNAUTHORIZED, ErrorMessage.UNAUTHORIZED.getMessage());
    }
}
