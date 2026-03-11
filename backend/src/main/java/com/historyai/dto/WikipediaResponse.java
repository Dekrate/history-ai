package com.historyai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for Wikipedia REST API.
 *
 * <p>Represents the summary information returned from the Wikipedia REST API
 * when fetching article content. This DTO is used to parse the JSON response
 * and extract relevant information about historical figures.</p>
 *
 * <p>The response includes:</p>
 * <ul>
 *   <li>Title - the article's title</li>
 *   <li>Extract - a summary/abstract of the article</li>
 *   <li>Description - brief description from Wikidata</li>
 *   <li>Wikibase item - Wikidata entity ID for additional data</li>
 *   <li>Thumbnail - image information (if available)</li>
 *   <li>Content URLs - links to the full article</li>
 * </ul>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see com.historyai.client.WikipediaApiClient
 * @see <a href="https://en.wikipedia.org/api/rest_v1/">Wikipedia REST API</a>
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
