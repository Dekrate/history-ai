package com.historyai.controller;

import com.historyai.dto.WikipediaResponse;
import com.historyai.service.WikipediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Wikipedia API integration.
 * Provides endpoints for retrieving historical character information.
 */
@RestController
@RequestMapping("/api/wikipedia")
@Tag(name = "Wikipedia", description = "Wikipedia API integration for historical characters")
public class WikipediaController {

    private static final Logger LOG = LoggerFactory.getLogger(WikipediaController.class);

    private final WikipediaService wikipediaService;

    /**
     * Constructs a new WikipediaController.
     *
     * @param wikipediaService the Wikipedia service
     */
    public WikipediaController(WikipediaService wikipediaService) {
        this.wikipediaService = wikipediaService;
    }

    /**
     * Gets character information from Wikipedia.
     *
     * @param characterName the name of the historical character
     * @return WikipediaResponse containing character information
     */
    @GetMapping("/{characterName}")
    @Operation(summary = "Get character info", description = "Fetches character summary from Wikipedia")
    public ResponseEntity<WikipediaResponse> getCharacterInfo(
            @Parameter(description = "Name of the historical character", required = true)
            @PathVariable String characterName) {
        LOG.info("Request for Wikipedia info: {}", characterName);
        WikipediaResponse response = wikipediaService.getCharacterInfo(characterName);
        return ResponseEntity.ok(response);
    }
}
