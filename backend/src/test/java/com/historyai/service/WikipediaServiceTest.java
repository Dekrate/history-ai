package com.historyai.service;

import com.historyai.client.WikipediaApiClient;
import com.historyai.client.WikidataApiClient;
import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.exception.WikipediaApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikipediaServiceTest {

    @Mock
    private WikipediaApiClient wikipediaApiClient;

    @Mock
    private WikidataApiClient wikidataApiClient;

    @InjectMocks
    private WikipediaService wikipediaService;

    private WikipediaResponse testResponse;

    @BeforeEach
    void setUp() {
        testResponse = new WikipediaResponse(
                "Mikołaj Kopernik",
                "Polish astronomer",
                "Renaissance astronomer",
                "Q123",
                null,
                null
        );
    }

    @Test
    void getCharacterInfo_WhenExists_ShouldReturnData() {
        when(wikipediaApiClient.getCharacterSummary("https://pl.wikipedia.org/api/rest_v1", "Kopernik"))
                .thenReturn(testResponse);
        when(wikidataApiClient.isHuman("Q123")).thenReturn(true);

        WikipediaResponse result = wikipediaService.getCharacterInfo("Kopernik");

        assertNotNull(result);
        assertEquals("Mikołaj Kopernik", result.title());
        verify(wikipediaApiClient, times(1))
                .getCharacterSummary("https://pl.wikipedia.org/api/rest_v1", "Kopernik");
        verify(wikidataApiClient, times(1)).isHuman("Q123");
    }

    @Test
    void getCharacterInfo_WhenNotFound_ShouldThrowException() {
        when(wikipediaApiClient.getCharacterSummary("https://pl.wikipedia.org/api/rest_v1", "Unknown"))
                .thenThrow(new CharacterNotFoundInWikipediaException("Unknown"));
        when(wikipediaApiClient.getCharacterSummary("https://en.wikipedia.org/api/rest_v1", "Unknown"))
                .thenThrow(new CharacterNotFoundInWikipediaException("Unknown"));

        assertThrows(CharacterNotFoundInWikipediaException.class, 
                () -> wikipediaService.getCharacterInfo("Unknown"));
    }

    @Test
    void getCharacterInfo_WhenApiError_ShouldThrowException() {
        when(wikipediaApiClient.getCharacterSummary("https://pl.wikipedia.org/api/rest_v1", "Error"))
                .thenThrow(new WikipediaApiException("API error", new RuntimeException()));
        when(wikipediaApiClient.getCharacterSummary("https://en.wikipedia.org/api/rest_v1", "Error"))
                .thenThrow(new WikipediaApiException("API error", new RuntimeException()));

        assertThrows(WikipediaApiException.class, 
                () -> wikipediaService.getCharacterInfo("Error"));
    }

    @Test
    void getCharacterInfo_WhenNotHuman_ShouldThrowException() {
        when(wikipediaApiClient.getCharacterSummary("https://pl.wikipedia.org/api/rest_v1", "Planeta"))
                .thenReturn(testResponse);
        when(wikipediaApiClient.getCharacterSummary("https://en.wikipedia.org/api/rest_v1", "Planeta"))
                .thenReturn(testResponse);
        when(wikidataApiClient.isHuman("Q123")).thenReturn(false);

        assertThrows(CharacterNotFoundInWikipediaException.class,
                () -> wikipediaService.getCharacterInfo("Planeta"));
    }

    @Test
    void getCharacterInfo_WhenPlNotHuman_ShouldFallbackToEnglish() {
        WikipediaResponse englishResponse = new WikipediaResponse(
                "Nicolaus Copernicus",
                "Polish astronomer",
                "Renaissance astronomer",
                "Q999",
                null,
                null
        );
        when(wikipediaApiClient.getCharacterSummary("https://pl.wikipedia.org/api/rest_v1", "Kopernik"))
                .thenReturn(testResponse);
        when(wikipediaApiClient.getCharacterSummary("https://en.wikipedia.org/api/rest_v1", "Kopernik"))
                .thenReturn(englishResponse);
        when(wikidataApiClient.isHuman("Q123")).thenReturn(false);
        when(wikidataApiClient.isHuman("Q999")).thenReturn(true);

        WikipediaResponse result = wikipediaService.getCharacterInfo("Kopernik");

        assertEquals("Nicolaus Copernicus", result.title());
    }

    @Test
    void getNationalityFromWikidata_WhenAvailable_ShouldReturnLabel() {
        when(wikidataApiClient.getFirstCitizenshipLabel("Q123", "pl", "en"))
                .thenReturn(java.util.Optional.of("Polska"));

        String nationality = wikipediaService.getNationalityFromWikidata("Q123");

        assertEquals("Polska", nationality);
    }
}
