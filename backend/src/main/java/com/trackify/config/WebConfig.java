package com.trackify.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Web layer configuration. Exposes the CORS bean consumed by the Spring Security
 * filter chain (TASK-024) so the SPA on {@code FRONTEND_ORIGIN} can call {@code /api/**}.
 */
@Configuration
public class WebConfig {

    @Value("${trackify.cors.allowed-origin}")
    private String allowedOrigin;

    @Value("${trackify.cors.allowed-methods}")
    private String[] allowedMethods;

    @Value("${trackify.cors.allowed-headers}")
    private String[] allowedHeaders;

    @Value("${trackify.cors.allow-credentials}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(Arrays.asList(allowedMethods));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders));
        config.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
