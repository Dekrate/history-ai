package com.historyai.repository;

import com.historyai.entity.HistoricalCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HistoricalCharacterRepository.
 */
@DataJpaTest
class HistoricalCharacterRepositoryTest {

    @Autowired
    private HistoricalCharacterRepository repository;

    private HistoricalCharacter testCharacter;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        
        testCharacter = new HistoricalCharacter("Mikołaj Kopernik", "Polish astronomer", "Renaissance", "Polish");
        testCharacter.setBirthYear(1473);
        testCharacter.setDeathYear(1543);
        testCharacter = repository.save(testCharacter);
    }

    @Test
    void findByName_WhenExists_ShouldReturnCharacter() {
        Optional<HistoricalCharacter> result = repository.findByName("Mikołaj Kopernik");

        assertTrue(result.isPresent());
        assertEquals("Mikołaj Kopernik", result.get().getName());
    }

    @Test
    void findByName_WhenNotExists_ShouldReturnEmpty() {
        Optional<HistoricalCharacter> result = repository.findByName("Unknown Person");

        assertFalse(result.isPresent());
    }

    @Test
    void findByEra_ShouldReturnMatchingCharacters() {
        List<HistoricalCharacter> results = repository.findByEra("Renaissance");

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(c -> "Renaissance".equals(c.getEra())));
    }

    @Test
    void findByEra_WhenNoMatch_ShouldReturnEmpty() {
        List<HistoricalCharacter> results = repository.findByEra("Modern");

        assertTrue(results.isEmpty());
    }

    @Test
    void findByNationality_ShouldReturnMatchingCharacters() {
        List<HistoricalCharacter> results = repository.findByNationality("Polish");

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(c -> "Polish".equals(c.getNationality())));
    }

    @Test
    void searchByName_ShouldFindMatchingCharacters() {
        List<HistoricalCharacter> results = repository.searchByName("Kopernik");

        assertFalse(results.isEmpty());
        assertTrue(results.get(0).getName().contains("Kopernik"));
    }

    @Test
    void searchByName_ShouldBeCaseInsensitive() {
        List<HistoricalCharacter> results = repository.searchByName("kopernik");

        assertFalse(results.isEmpty());
    }

    @Test
    void searchByName_WhenNoMatch_ShouldReturnEmpty() {
        List<HistoricalCharacter> results = repository.searchByName("xyz123");

        assertTrue(results.isEmpty());
    }

    @Test
    void existsByName_WhenExists_ShouldReturnTrue() {
        boolean exists = repository.existsByName("Mikołaj Kopernik");

        assertTrue(exists);
    }

    @Test
    void existsByName_WhenNotExists_ShouldReturnFalse() {
        boolean exists = repository.existsByName("Unknown Person");

        assertFalse(exists);
    }

    @Test
    void save_ShouldPersistCharacter() {
        HistoricalCharacter newCharacter = new HistoricalCharacter(
                "Jan III Sobieski", "Polish king", "Renaissance", "Polish");
        newCharacter.setBirthYear(1629);
        newCharacter.setDeathYear(1696);

        HistoricalCharacter saved = repository.save(newCharacter);

        assertNotNull(saved.getId());
        assertEquals("Jan III Sobieski", saved.getName());
    }

    @Test
    void delete_ShouldRemoveCharacter() {
        UUID id = testCharacter.getId();
        repository.delete(testCharacter);

        Optional<HistoricalCharacter> result = repository.findById(id);
        assertFalse(result.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllCharacters() {
        List<HistoricalCharacter> results = repository.findAll();

        assertFalse(results.isEmpty());
    }
}
