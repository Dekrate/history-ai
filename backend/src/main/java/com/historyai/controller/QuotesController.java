package com.historyai.controller;

import com.historyai.service.WikiquoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for debugging Wikiquote integration.
 */
@RestController
@RequestMapping("/api/quotes")
@Tag(name = "Quotes", description = "Wikiquote quote retrieval")
public class QuotesController {

    private static final Logger LOG = LoggerFactory.getLogger(QuotesController.class);

    private final WikiquoteService wikiquoteService;

    public QuotesController(WikiquoteService wikiquoteService) {
        this.wikiquoteService = wikiquoteService;
    }

    @GetMapping
    @Operation(summary = "Get quotes", description = "Fetches quotes from Wikiquote (PL then EN)")
    public ResponseEntity<List<String>> getQuotes(
            @Parameter(description = "Name of the person", required = true)
            @RequestParam String name,
            @Parameter(description = "Language preference (pl or en)")
            @RequestParam(required = false, defaultValue = "pl") String lang) {
        LOG.info("Request for quotes: {} (lang={})", name, lang);
        List<String> quotes = wikiquoteService.getQuotes(name, lang);
        return ResponseEntity.ok(quotes);
    }
}
