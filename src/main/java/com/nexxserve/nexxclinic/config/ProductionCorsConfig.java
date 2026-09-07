package com.nexxserve.nexxclinic.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile("prod")
public class ProductionCorsConfig {

    /**
     * Comma-separated list of allowed origin patterns. Defaults to both schemes
     * for *.med.rw so the app works whether it is served over http or https
     * (e.g. Cloudflare with "Always Use HTTPS" still not yet fully enforced).
     * Browsers send an Origin header on every POST, so if the site is reached
     * via http the backend MUST allow the http scheme or requests get rejected
     * with 403 "Invalid CORS request".
     */
    @Value("${CORS_ALLOWED_ORIGINS:https://*.med.rw,http://*.med.rw}")
    private String allowedOriginsEnv;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = Arrays.asList(allowedOriginsEnv.split(","));
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowCredentials(true);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
