package com.historyai.service;

import com.historyai.client.OllamaClient;
import com.historyai.dto.FactCheckResult;
import com.historyai.dto.WikipediaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactCheckServiceTest {

    @Mock
    private OllamaClient ollamaClient;

    @Mock
    private WikipediaService wikipediaService;

    @InjectMocks
    private FactCheckService factCheckService;

    private WikipediaResponse wikiResponse;

    @BeforeEach
    void setUp() {
        wikiResponse = new WikipediaResponse(
                "Nicolaus Copernicus",
                "Polish astronomer",
                "Renaissance mathematician",
                null,
                null
        );
    }

    @Test
    void factCheck_WithValidClaim_ShouldReturnVerified() {
        when(wikipediaService.getCharacterInfo("Mikołaj Kopernik"))
                .thenReturn(wikiResponse);
        when(ollamaClient.generate(anyString()))
                .thenReturn("VERIFICATION: TRUE\nCONFIDENCE: 0.95\nEXPLANATION: This is correct.");

        List<FactCheckResult> results = factCheckService.factCheck(
                "Mikołaj Kopernik urodził się w 1473 roku.",
                "Mikołaj Kopernik"
        );

        assertEquals(1, results.size());
        assertEquals(FactCheckResult.VerificationResult.VERIFIED, results.get(0).getVerification());
        assertEquals(0.95f, results.get(0).getConfidence());
    }

    @Test
    void factCheck_WithFalseClaim_ShouldReturnFalse() {
        when(wikipediaService.getCharacterInfo("Mikołaj Kopernik"))
                .thenReturn(wikiResponse);
        when(ollamaClient.generate(anyString()))
                .thenReturn("VERIFICATION: FALSE\nCONFIDENCE: 0.9\nEXPLANATION: Wrong year.");

        List<FactCheckResult> results = factCheckService.factCheck(
                "Kopernik urodził się w 1500 roku.",
                "Mikołaj Kopernik"
        );

        assertEquals(1, results.size());
        assertEquals(FactCheckResult.VerificationResult.FALSE, results.get(0).getVerification());
    }

    @Test
    void verifyClaim_WithValidData_ShouldParseCorrectly() {
        when(wikipediaService.getCharacterInfo("Test"))
                .thenReturn(wikiResponse);
        when(ollamaClient.generate(anyString()))
                .thenReturn("VERIFICATION: TRUE\nCONFIDENCE: 0.85\nEXPLANATION: Test explanation.");

        FactCheckResult result = factCheckService.verifyClaim(
                "Test claim.",
                "Test"
        );

        assertEquals(FactCheckResult.VerificationResult.VERIFIED, result.getVerification());
        assertEquals(0.85f, result.getConfidence());
        assertEquals("Test explanation.", result.getExplanation());
    }

    @Test
    void verifyClaim_WithFalseVerification_ShouldReturnFalse() {
        when(wikipediaService.getCharacterInfo("Test"))
                .thenReturn(wikiResponse);
        when(ollamaClient.generate(anyString()))
                .thenReturn("VERIFICATION: FALSE\nCONFIDENCE: 0.8\nEXPLANATION: Wrong info.");

        FactCheckResult result = factCheckService.verifyClaim("Test.", "Test");

        assertEquals(FactCheckResult.VerificationResult.FALSE, result.getVerification());
    }

    @Test
    void verifyClaim_WithNoVerificationKeyword_ShouldReturnUnverifiable() {
        when(wikipediaService.getCharacterInfo("Test"))
                .thenReturn(wikiResponse);
        when(ollamaClient.generate(anyString()))
                .thenReturn("Some random response without verification keyword.");

        FactCheckResult result = factCheckService.verifyClaim("Test.", "Test");

        assertEquals(FactCheckResult.VerificationResult.UNVERIFIABLE, result.getVerification());
    }
}
