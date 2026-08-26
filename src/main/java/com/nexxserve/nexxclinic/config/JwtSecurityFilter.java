package com.nexxserve.nexxclinic.config;

import com.nexxserve.nexxclinic.auth.AccessTokenInfo;
import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.auth.JwtService;
import com.nexxserve.nexxclinic.service.SessionTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts JWT from the Authorization header and populates the Spring Security
 * {@code SecurityContext} so that {@code .authenticated()} works for REST endpoints.
 *
 * <p>GraphQL auth is still handled by {@link GraphQlAuthInterceptor} — this filter
 * only fires for non-GraphQL HTTP requests (REST controllers, actuator, etc.).
 */
@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtSecurityFilter.class);

    private final JwtService jwtService;
    private final SessionTokenService sessionTokenService;

    public JwtSecurityFilter(JwtService jwtService, SessionTokenService sessionTokenService) {
        this.jwtService = jwtService;
        this.sessionTokenService = sessionTokenService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip if already authenticated (e.g. from another filter)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                AccessTokenInfo tokenInfo = jwtService.parseAccessToken(token);

                if (!sessionTokenService.isAccessTokenRevoked(tokenInfo.tokenId())) {
                    // Convert roles to Spring Security authorities
                    var authorities = tokenInfo.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                            .toList();

                    AuthenticatedUser principal = jwtService.toAuthenticatedUser(tokenInfo);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                log.debug("JWT validation failed for REST request: {}", e.getMessage());
                // Not authenticated — let Spring Security handle the 401
            }
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't filter GraphQL — handled by GraphQlAuthInterceptor
        return path.equals("/graphql") || path.startsWith("/graphiql");
    }
}
