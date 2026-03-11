package com.historyai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration for Cross-Origin Resource Sharing (CORS).
 *
 * <p>Configures CORS to allow the frontend application to make API requests
 * to this backend. The allowed origins, methods, and headers are configurable
 * via application properties.</p>
 *
 * <p>Default configuration allows:</p>
 * <ul>
 *   <li>Common HTTP methods (GET, POST, PUT, DELETE, OPTIONS)</li>
 *   <li>Common headers (Authorization, Content-Type, X-Trace-Id)</li>
 *   <li>Credentials support</li>
 * </ul>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">MDN CORS Guide</a>
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;
    private final List<String> allowedMethods;
    private final List<String> allowedHeaders;

    public CorsConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String allowedOrigins,
            @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}") String allowedMethods,
            @Value("${app.cors.allowed-headers:*}") String allowedHeaders) {
        this.allowedOrigins = parseCsv(allowedOrigins);
        this.allowedMethods = parseCsv(allowedMethods);
        this.allowedHeaders = parseCsv(allowedHeaders);
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        if (allowedOrigins.isEmpty()) {
            config.addAllowedOriginPattern("*");
            config.setAllowCredentials(false);
        } else {
            allowedOrigins.forEach(config::addAllowedOrigin);
            config.setAllowCredentials(true);
        }
        allowedMethods.forEach(config::addAllowedMethod);
        allowedHeaders.forEach(config::addAllowedHeader);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    private List<String> parseCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
