package com.leedahun.storecaseidentity.domain.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leedahun.storecaseidentity.common.response.HttpResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationResponseUtil {

    public static void authenticateFail(HttpServletResponse response,
                                        HttpStatus httpStatus,
                                        String message) throws IOException, ServletException {
        HttpResponse httpResponse = new HttpResponse(httpStatus, message, null);
        String responseData = new ObjectMapper().writeValueAsString(httpResponse);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(responseData);
    }
}
