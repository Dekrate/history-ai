package com.historyai.integration;

import com.historyai.dto.HistoricalCharacterDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Character API endpoints using Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
class CharacterApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("history_ai")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;
    private static UUID testCharacterId;

    @BeforeAll
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/characters";
    }

    @Test
    @Order(1)
    void createCharacter_ShouldReturn201() {
        HistoricalCharacterDTO dto = new HistoricalCharacterDTO();
        dto.setName("Mikołaj Kopernik");
        dto.setBirthYear(1473);
        dto.setDeathYear(1543);
        dto.setBiography("Polish astronomer");
        dto.setEra("Renaissance");
        dto.setNationality("Polish");

        HttpEntity<HistoricalCharacterDTO> request = new HttpEntity<>(dto);
        ResponseEntity<HistoricalCharacterDTO> response = restTemplate.postForEntity(
                baseUrl, request, HistoricalCharacterDTO.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("Mikołaj Kopernik", response.getBody().getName());
        
        testCharacterId = response.getBody().getId();
    }

    @Test
    @Order(2)
    void getCharacterById_ShouldReturnCharacter() {
        ResponseEntity<HistoricalCharacterDTO> response = restTemplate.getForEntity(
                baseUrl + "/" + testCharacterId, HistoricalCharacterDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testCharacterId, response.getBody().getId());
    }

    @Test
    @Order(3)
    void getAllCharacters_ShouldReturnList() {
        ResponseEntity<HistoricalCharacterDTO[]> response = restTemplate.getForEntity(
                baseUrl, HistoricalCharacterDTO[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    @Order(4)
    void searchCharacters_ShouldReturnMatching() {
        ResponseEntity<HistoricalCharacterDTO[]> response = restTemplate.getForEntity(
                baseUrl + "/search?q=Kopernik", HistoricalCharacterDTO[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    @Order(5)
    void updateCharacter_ShouldReturn200() {
        HistoricalCharacterDTO dto = new HistoricalCharacterDTO();
        dto.setName("Mikołaj Kopernik - Updated");
        dto.setBirthYear(1473);
        dto.setDeathYear(1543);
        dto.setBiography("Updated biography");
        dto.setEra("Renaissance");
        dto.setNationality("Polish");

        HttpEntity<HistoricalCharacterDTO> request = new HttpEntity<>(dto);
        ResponseEntity<HistoricalCharacterDTO> response = restTemplate.exchange(
                baseUrl + "/" + testCharacterId,
                HttpMethod.PUT,
                request,
                HistoricalCharacterDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mikołaj Kopernik - Updated", response.getBody().getName());
    }

    @Test
    @Order(6)
    void deleteCharacter_ShouldReturn204() {
        restTemplate.delete(baseUrl + "/" + testCharacterId);

        ResponseEntity<HistoricalCharacterDTO> getResponse = restTemplate.getForEntity(
                baseUrl + "/" + testCharacterId, HistoricalCharacterDTO.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    void createCharacter_InvalidYears_ShouldReturn400() {
        HistoricalCharacterDTO dto = new HistoricalCharacterDTO();
        dto.setName("Test Character");
        dto.setBirthYear(1600);
        dto.setDeathYear(1500);

        HttpEntity<HistoricalCharacterDTO> request = new HttpEntity<>(dto);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl, request, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getCharacter_NotFound_ShouldReturn404() {
        UUID randomId = UUID.randomUUID();
        ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/" + randomId, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
