package com.leedahun.storecaseidentity.domain.auth.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.leedahun.storecaseidentity.domain.auth.config.JwtProperties;
import com.leedahun.storecaseidentity.domain.auth.constant.JwtConstants;
import com.leedahun.storecaseidentity.domain.auth.dto.LoginUser;
import com.leedahun.storecaseidentity.domain.auth.entity.Role;
import com.leedahun.storecaseidentity.domain.auth.util.JwtUtil;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthorizationFilterTest {

    private JwtAuthorizationFilter filter;
    private JwtUtil jwtUtil;

    private static final String SECRET = "test-secret-512";
    private static final long ACCESS_EXP_MS = 60_000L;
    private static final long REFRESH_EXP_MS = 86_400_000L;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setExpirationTime(ACCESS_EXP_MS);
        props.setRefreshExpirationTime(REFRESH_EXP_MS);

        jwtUtil = new JwtUtil(props);
        filter = new JwtAuthorizationFilter(jwtUtil) {
            {
                WHITELIST = List.of("/api/auth/join", "/api/auth/login");
            }
        };

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String makeValidAuthHeader(Long userId, Role role) {
        return jwtUtil.createAccessToken(userId, role);
    }

    private String makeForgedAuthHeader(Long userId, Role role) {
        String invalidToken = JWT.create()
                .withSubject(JwtConstants.CLAIM_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
                .withClaim(JwtConstants.CLAIM_ID, userId)
                .withClaim(JwtConstants.CLAIM_ROLE, role.name())
                .sign(Algorithm.HMAC512("DIFFERENT_SECRET"));
        return JwtConstants.TOKEN_PREFIX + invalidToken;
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 체인 통과하고 인증을 설정하지 않는다")
    void noAuthorizationHeader_passThrough_noAuth() throws ServletException, IOException {
        // given
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(req, res, chain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증이 설정된다")
    void validToken_setsAuthentication() throws ServletException, IOException {
        // given
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.addHeader(HttpHeaders.AUTHORIZATION, makeValidAuthHeader(42L, Role.USER));
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(req, res, chain);

        // then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(LoginUser.class);

        LoginUser principal = (LoginUser) auth.getPrincipal();
        assertThat(principal.getId()).isEqualTo(42L);
        assertThat(principal.getRole()).isEqualTo(Role.USER.name());
    }

    @Test
    @DisplayName("잘못 서명된 토큰이면 CustomException 처리로 상태와 바디가 설정되고 체인이 중단된다")
    void forgedToken_writesErrorAndStopsChain() throws ServletException, IOException {
        // given
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/orders");
        req.addHeader(HttpHeaders.AUTHORIZATION, makeForgedAuthHeader(7L, Role.USER));
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(req, res, chain);

        // then
        assertThat(res.getStatus()).isIn(401, 403);
        assertThat(res.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(res.getContentAsString()).isNotEmpty();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("OPTIONS 요청(CORS 확인)은 shouldNotFilter로 스킵된다")
    void optionsRequest_skippedByShouldNotFilter() throws ServletException, IOException {
        // given
        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/api/anything");
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // when
        filter.doFilter(req, res, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(res.getStatus()).isEqualTo(200);
        assertThat(res.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("화이트리스트 경로는 스킵된다")
    void whitelistPaths_areSkipped() throws ServletException, IOException {
        for (String path : new String[]{"/api/auth/login", "/api/auth/join"}) {
            // given
            MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
            req.addHeader(HttpHeaders.AUTHORIZATION, makeValidAuthHeader(99L, Role.ADMIN));

            MockHttpServletResponse res = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            // when
            filter.doFilter(req, res, chain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(res.getStatus()).isEqualTo(200);
            assertThat(res.getContentAsString()).isEmpty();

            SecurityContextHolder.clearContext();
        }
    }
}