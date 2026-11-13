package com.eex.infrastructure.security;

import com.eex.domain.service.UserService;
import com.eex.domain.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lit l'en-tête "Authorization: Bearer &lt;jwt&gt;", valide le jeton et, si l'utilisateur
 * existe toujours en base, l'authentifie pour la requête courante.
 * Sans jeton valide, la requête reste anonyme et sera rejetée par la SecurityFilterChain.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Integer userId = jwtUtil.validateAndGetUserId(header.substring(BEARER_PREFIX.length()));
            if (userId != null) {
                userService.getUserById(userId).ifPresent(user -> {
                    AuthenticatedUser principal =
                            new AuthenticatedUser(user.getId(), user.getUsername(), user.getClientId());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, AuthorityUtils.createAuthorityList("ROLE_CLIENT"));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
