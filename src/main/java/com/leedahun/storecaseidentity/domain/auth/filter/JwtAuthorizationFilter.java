package com.leedahun.storecaseidentity.domain.auth.filter;

import com.leedahun.storecaseidentity.common.error.exception.CustomException;
import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import com.leedahun.storecaseidentity.domain.auth.util.AuthenticationResponseUtil;
import com.leedahun.storecaseidentity.domain.auth.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // 아래 url들은 jwt 토큰 검사를 패스함
    protected static List<String> WHITELIST = List.of(
            "/api/auth/login",
            "/api/auth/join",
            "/api/auth/join/email",
            "/api/auth/join/email/verify"
    );
    private static final AntPathMatcher PATH = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 브라우저가 CORS 확인할 때 OPTIONS 요청을 먼저 보내는데 이건 JWT 검증 같은 인증 절차를 거치면 안됨
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // true 리턴시 doFilterInternal을 거치지 않음.
        String uri = request.getRequestURI();
        return WHITELIST.stream().anyMatch(p -> PATH.match(p, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String token = resolveBearerToken(req);
        if (!StringUtils.hasText(token)) {
            chain.doFilter(req, res);
            return;
        }

        try {
            LoginUser principal = jwtUtil.verify(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());

                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
        } catch (CustomException e) {
            res.setStatus(e.getStatus().value());
            AuthenticationResponseUtil.authenticateFail(res, e.getStatus(), e.getMessage());
            return;
        }

        chain.doFilter(req, res);
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header)) {
            return null;
        }

        if (!header.startsWith(JwtConstants.TOKEN_PREFIX)) {
            return null;
        }
        return header.substring(JwtConstants.TOKEN_PREFIX.length()).trim();
    }
}
