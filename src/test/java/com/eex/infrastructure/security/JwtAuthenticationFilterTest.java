package com.eex.infrastructure.security;

import com.eex.domain.entity.User;
import com.eex.domain.service.UserService;
import com.eex.domain.util.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserService userService;
    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenAuthenticatesUserWithItsClient() throws Exception {
        request.addHeader("Authorization", "Bearer good-token");
        when(jwtUtil.validateAndGetUserId("good-token")).thenReturn(2);
        when(userService.getUserById(2)).thenReturn(Optional.of(new User(2, "Bernard", "hash", 2)));

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(new AuthenticatedUser(2, "Bernard", 2));
        assertThat(auth.getCredentials()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingHeaderLeavesRequestAnonymous() throws Exception {
        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil, userService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void nonBearerHeaderLeavesRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Basic YWxpY2U6c2VjcmV0");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void invalidTokenLeavesRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer bad-token");
        when(jwtUtil.validateAndGetUserId("bad-token")).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenOfDeletedUserLeavesRequestAnonymous() throws Exception {
        request.addHeader("Authorization", "Bearer token");
        when(jwtUtil.validateAndGetUserId("token")).thenReturn(99);
        when(userService.getUserById(99)).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
