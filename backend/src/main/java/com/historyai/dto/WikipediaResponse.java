package com.historyai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WikipediaResponse(
    @JsonProperty("title")
    String title,
    @JsonProperty("extract")
    String extract,
    @JsonProperty("description")
    String description,
    @JsonProperty("thumbnail")
    Thumbnail thumbnail,
    @JsonProperty("content_urls")
    ContentUrls contentUrls
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Thumbnail(
        @JsonProperty("source")
        String source,
        @JsonProperty("width")
        int width,
        @JsonProperty("height")
        int height
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentUrls(
        @JsonProperty("desktop")
        UrlInfo desktop,
        @JsonProperty("mobile")
        UrlInfo mobile
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UrlInfo(
        @JsonProperty("page")
        String page
    ) {}
}
