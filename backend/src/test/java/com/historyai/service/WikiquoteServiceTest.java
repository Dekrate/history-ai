package com.historyai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.historyai.client.WikiquoteApiClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WikiquoteServiceTest {

    @Mock
    private WikiquoteApiClient wikiquoteApiClient;

    @InjectMocks
    private WikiquoteService wikiquoteService;

    private String extract;

    @BeforeEach
    void setUp() {
        extract = """
            Cytaty
            * Pierwszy cytat.
            * Drugi cytat.
            - Trzeci cytat.
            """;
    }

    @Test
    void getQuotes_WhenPlExists_ShouldReturnQuotes() {
        when(wikiquoteApiClient.getPageWikitext("https://pl.wikiquote.org/w/api.php", "Jan Paweł II"))
                .thenReturn(extract);

        List<String> quotes = wikiquoteService.getQuotes("Jan Paweł II");

        assertEquals(3, quotes.size());
        assertEquals("Pierwszy cytat.", quotes.get(0));
    }

    @Test
    void getQuotes_WhenPlMissing_ShouldFallbackToEn() {
        when(wikiquoteApiClient.getPageWikitext("https://pl.wikiquote.org/w/api.php", "Copernicus"))
                .thenReturn(null);
        when(wikiquoteApiClient.getPageWikitext("https://en.wikiquote.org/w/api.php", "Copernicus"))
                .thenReturn("* Quote.");

        List<String> quotes = wikiquoteService.getQuotes("Copernicus");

        assertEquals(1, quotes.size());
        assertEquals("Quote.", quotes.get(0));
    }

    @Test
    void getQuotes_WhenNoQuotes_ShouldReturnEmpty() {
        when(wikiquoteApiClient.getPageWikitext("https://pl.wikiquote.org/w/api.php", "Unknown"))
                .thenReturn("Brak cytatów.");
        when(wikiquoteApiClient.getPageWikitext("https://en.wikiquote.org/w/api.php", "Unknown"))
                .thenReturn(null);

        List<String> quotes = wikiquoteService.getQuotes("Unknown");

        assertTrue(quotes.isEmpty());
    }

    @Test
    void getQuotes_ShouldSkipMetadataLines() {
        String wikitext = """
            * Pierwszy cytat.
            * Opis: wyjaśnienie.
            * Źródło: książka.
            * Zobacz też: inne.
            """;
        when(wikiquoteApiClient.getPageWikitext("https://pl.wikiquote.org/w/api.php", "Test"))
                .thenReturn(wikitext);

        List<String> quotes = wikiquoteService.getQuotes("Test");

        assertEquals(1, quotes.size());
        assertEquals("Pierwszy cytat.", quotes.get(0));
    }
}
