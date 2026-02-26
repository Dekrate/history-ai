package com.historyai.integration;

import com.historyai.dto.WikipediaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "resilience4j.ratelimiter.instances.wikipedia.enabled=false"
})
class WikipediaApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getCharacterInfo_WhenValidCharacter_ShouldReturn200() {
        String url = "http://localhost:" + port + "/api/wikipedia/Nicolaus_Copernicus";

        ResponseEntity<WikipediaResponse> response = restTemplate.getForEntity(
                url, WikipediaResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().title());
    }

    @Test
    void getCharacterInfo_WhenInvalidCharacter_ShouldReturn404() {
        String url = "http://localhost:" + port + "/api/wikipedia/ThisCharacterDoesNotExist123456789";

        ResponseEntity<String> response = restTemplate.getForEntity(
                url, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
