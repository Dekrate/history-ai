package com.historyai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main entry point for the HistoryAI Spring Boot application.
 *
 * <p>AI-powered educational platform enabling conversations with historical figures.
 * The application provides:</p>
 * <ul>
 *   <li>RESTful API for character management</li>
 *   <li>Real-time chat with AI-powered historical personas</li>
 *   <li>Fact-checking against Wikipedia sources</li>
 *   <li>Quote enrichment from Wikiquote</li>
 * </ul>
 *
 * <p>Enabled features:</p>
 * <ul>
 *   <li>{@link SpringBootApplication} - Spring Boot auto-configuration</li>
 *   <li>{@link EnableJpaAuditing} - Automatic timestamp management</li>
 *   <li>{@link EnableCaching} - Response caching for API calls</li>
 * </ul>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see <a href="https://github.com/Dekrate/history-ai">Project Repository</a>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
public class HistoryAiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HistoryAiApplication.class, args);
    }
}
