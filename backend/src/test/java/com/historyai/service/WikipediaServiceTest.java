package com.historyai.service;

import com.historyai.client.WikipediaApiClient;
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

    @InjectMocks
    private WikipediaService wikipediaService;

    private WikipediaResponse testResponse;

    @BeforeEach
    void setUp() {
        testResponse = new WikipediaResponse(
                "Mikołaj Kopernik",
                "Polish astronomer",
                "Renaissance astronomer",
                null,
                null
        );
    }

    @Test
    void getCharacterInfo_WhenExists_ShouldReturnData() {
        when(wikipediaApiClient.getCharacterSummary("Kopernik")).thenReturn(testResponse);

        WikipediaResponse result = wikipediaService.getCharacterInfo("Kopernik");

        assertNotNull(result);
        assertEquals("Mikołaj Kopernik", result.title());
        verify(wikipediaApiClient, times(1)).getCharacterSummary("Kopernik");
    }

    @Test
    void getCharacterInfo_WhenNotFound_ShouldThrowException() {
        when(wikipediaApiClient.getCharacterSummary("Unknown"))
                .thenThrow(new CharacterNotFoundInWikipediaException("Unknown"));

        assertThrows(CharacterNotFoundInWikipediaException.class, 
                () -> wikipediaService.getCharacterInfo("Unknown"));
    }

    @Test
    void getCharacterInfo_WhenApiError_ShouldThrowException() {
        when(wikipediaApiClient.getCharacterSummary("Error"))
                .thenThrow(new WikipediaApiException("API error", new RuntimeException()));

        assertThrows(WikipediaApiException.class, 
                () -> wikipediaService.getCharacterInfo("Error"));
    }
}
