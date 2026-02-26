package com.historyai.integration;

import com.historyai.dto.FactCheckRequest;
import com.historyai.dto.FactCheckResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
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
class FactCheckApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void factCheck_ValidRequest_ShouldReturn200() {
        String url = "http://localhost:" + port + "/api/factcheck";
        
        FactCheckRequest request = new FactCheckRequest(
                "Mikołaj Kopernik urodził się w 1473 roku w Toruniu.",
                "Mikołaj Kopernik"
        );
        
        HttpEntity<FactCheckRequest> httpRequest = new HttpEntity<>(request);
        
        ResponseEntity<FactCheckResult[]> response = restTemplate.postForEntity(
                url, httpRequest, FactCheckResult[].class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void factCheck_WithEmptyMessage_ShouldReturn200() {
        String url = "http://localhost:" + port + "/api/factcheck";
        
        FactCheckRequest request = new FactCheckRequest("", "Test");
        
        HttpEntity<FactCheckRequest> httpRequest = new HttpEntity<>(request);
        
        ResponseEntity<FactCheckResult[]> response = restTemplate.postForEntity(
                url, httpRequest, FactCheckResult[].class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void factCheck_WithoutCharacterContext_ShouldReturn200() {
        String url = "http://localhost:" + port + "/api/factcheck";
        
        FactCheckRequest request = new FactCheckRequest(
                "Jan III Sobieski był królem Polski.",
                null
        );
        
        HttpEntity<FactCheckRequest> httpRequest = new HttpEntity<>(request);
        
        ResponseEntity<FactCheckResult[]> response = restTemplate.postForEntity(
                url, httpRequest, FactCheckResult[].class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
