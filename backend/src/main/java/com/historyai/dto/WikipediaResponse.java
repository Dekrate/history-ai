package com.historyai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Wikipedia REST API.
 * Contains summary information about a Wikipedia article.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WikipediaResponse(
		@JsonProperty("title")
    String title,
    
    /* The extract/summary of the article */
    @JsonProperty("extract")
    String extract,
    
    /* Brief description of the article */
    @JsonProperty("description")
    String description,

    /* Wikidata item ID (e.g., Q12345) */
    @JsonProperty("wikibase_item")
    String wikibaseItem,
    
    /* Thumbnail image information */
    @JsonProperty("thumbnail")
    Thumbnail thumbnail,
    
    /* Content URLs for the article */
    @JsonProperty("content_urls")
    ContentUrls contentUrls
) {

    /**
     * Thumbnail information containing image source and dimensions.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnail(
        /* URL of the thumbnail image */
        @JsonProperty("source")
        String source,
        /* Width of the image */
        @JsonProperty("width")
        int width,
        /* Height of the image */
        @JsonProperty("height")
        int height
    ) {}

    /**
     * Content URLs containing desktop and mobile page links.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentUrls(
        /* Desktop URL information */
        @JsonProperty("desktop")
        UrlInfo desktop,
        /* Mobile URL information */
        @JsonProperty("mobile")
        UrlInfo mobile
    ) {}

    /**
     * URL information for a specific platform.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UrlInfo(
        /* The URL string */
        @JsonProperty("page")
        String page
    ) {}
}
