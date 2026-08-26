package com.nexxserve.nexxclinic.config;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    private final JwtSecurityFilter jwtSecurityFilter;

    public WebSecurityConfig(JwtSecurityFilter jwtSecurityFilter) {
        this.jwtSecurityFilter = jwtSecurityFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security filter chain");
        
        return http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtSecurityFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiResponse body = ApiResponse.unauthenticated(
                                    "Authentication required. Please provide a valid Bearer token.");
                            response.getWriter().write(
                                    new ObjectMapper().writeValueAsString(body));
                        })
                )
                .authorizeHttpRequests(registry -> {
                    // ── Public endpoints ──────────────────────────────────────
                    // GraphQL auth is handled by GraphQlAuthInterceptor + @HasRole,
                    // not by Spring Security's filter chain, so it must stay open.
                    registry.requestMatchers("/graphql").permitAll();
                    registry.requestMatchers("/graphiql/**").permitAll();
                    // Actuator health/info are public for load balancers / probes
                    registry.requestMatchers("/actuator/health").permitAll();
                    registry.requestMatchers("/actuator/info").permitAll();
                    // Media serving is public by design (patient photos, documents)
                    registry.requestMatchers("/api/media/**").permitAll();
                    // ── Protected REST endpoints ───────────────────────────────
                    // Upload and any other REST mutation require authentication.
                    // Spring Security will return 401 if no valid Bearer token.
                    registry.anyRequest().authenticated();
                })
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }
}
