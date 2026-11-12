package com.tasktracker.backend.security;

import com.tasktracker.backend.dto.CurrentUser;
import com.tasktracker.backend.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Юнит-тесты фильтра: настоящий JwtService, запрос и цепочка — заглушки Spring MVC. */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-only-jwt-secret-0123456789-abcdefghij";
    private static final UUID USER_ID = UUID.fromString("018f6d2e-1b7f-7cc1-9f31-3f6b8a2e4c5d");

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/tasks");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesRequestWithValidToken() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issue(USER_ID, "user@example.com"));

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isEqualTo(new CurrentUser(USER_ID, "user@example.com"));
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void schemeNameIsCaseInsensitive() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + jwtService.issue(USER_ID, "user@example.com"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void extraSpacesAroundTokenAreIgnored() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer    " + jwtService.issue(USER_ID, "user@example.com"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    void requestWithoutAuthorizationHeaderStaysAnonymous() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void otherAuthorizationSchemeIsIgnored() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidTokenStaysAnonymous() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void emptyTokenStaysAnonymous() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer    ");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
