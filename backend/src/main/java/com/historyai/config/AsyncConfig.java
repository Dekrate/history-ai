package com.historyai.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for asynchronous task execution.
 *
 * <p>Configures thread pools for handling asynchronous operations,
 * particularly for streaming responses that require long-running tasks.</p>
 *
 * <p>Uses Java virtual threads (available in Java 21+) for efficient
 * handling of concurrent streaming requests.</p>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see java.util.concurrent.Executor
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "streamingExecutor")
    public Executor streamingExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
