package com.historyai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring configuration that sets up the OpenAPI/Swagger metadata for the HistoryAI REST API.
 * <p>
 * Exposes the generated OpenAPI specification (e.g. at {@code /v3/api-docs}) and the
 * interactive Swagger UI (commonly available at {@code /swagger-ui/index.html}),
 * allowing developers to explore and test the API endpoints.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI historyAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HistoryAI API")
                        .description("AI-powered educational platform for conversations with historical figures. "
                                + "Provides character management, real-time chat streaming, and fact-checking capabilities.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HistoryAI Team")
                                .url("https://github.com/Dekrate/history-ai"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")))
                .components(new Components());
    }
}
