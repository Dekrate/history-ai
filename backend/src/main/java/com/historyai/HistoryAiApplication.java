package com.historyai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HistoryAiApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HistoryAiApplication.class, args);
    }
}
