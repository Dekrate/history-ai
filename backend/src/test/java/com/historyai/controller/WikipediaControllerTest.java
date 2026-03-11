package com.historyai.controller;

import com.historyai.dto.WikipediaResponse;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.exception.WikipediaApiException;
import com.historyai.service.WikipediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikipediaControllerTest {

    @Mock
    private WikipediaService wikipediaService;

    @InjectMocks
    private WikipediaController controller;

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
    void getCharacterInfo_WhenExists_ShouldReturn200() {
        when(wikipediaService.getCharacterInfo("Kopernik")).thenReturn(testResponse);

        ResponseEntity<WikipediaResponse> response = controller.getCharacterInfo("Kopernik");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mikołaj Kopernik", response.getBody().title());
    }

    @Test
    void getCharacterInfo_WhenNotFound_ShouldThrow404() {
        when(wikipediaService.getCharacterInfo("Unknown"))
                .thenThrow(new CharacterNotFoundInWikipediaException("Unknown"));

        assertThrows(CharacterNotFoundInWikipediaException.class,
                () -> controller.getCharacterInfo("Unknown"));
    }

    @Test
    void getCharacterInfo_WhenApiError_ShouldThrow503() {
        when(wikipediaService.getCharacterInfo("Error"))
                .thenThrow(new WikipediaApiException("Service unavailable", new RuntimeException()));

        assertThrows(WikipediaApiException.class,
                () -> controller.getCharacterInfo("Error"));
    }
}
