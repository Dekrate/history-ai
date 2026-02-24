package com.historyai.service;

import com.historyai.dto.HistoricalCharacterDTO;
import com.historyai.entity.HistoricalCharacter;
import com.historyai.repository.HistoricalCharacterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock
    private HistoricalCharacterRepository repository;

    @InjectMocks
    private CharacterService characterService;

    private HistoricalCharacter testCharacter;
    private HistoricalCharacterDTO testDTO;

    @BeforeEach
    void setUp() {
        String id = UUID.randomUUID().toString();
        testCharacter = new HistoricalCharacter("Mikołaj Kopernik", "Polski astronom", "Renesans", "Polska");
        testCharacter.setId(id);
        testCharacter.setBirthYear(1473);
        testCharacter.setDeathYear(1543);

        testDTO = new HistoricalCharacterDTO();
        testDTO.setId(id);
        testDTO.setName("Mikołaj Kopernik");
        testDTO.setBirthYear(1473);
        testDTO.setDeathYear(1543);
        testDTO.setBiography("Polski astronom");
        testDTO.setEra("Renesans");
        testDTO.setNationality("Polska");
    }

    @Test
    void findAll_ShouldReturnAllCharacters() {
        when(repository.findAll()).thenReturn(Arrays.asList(testCharacter));

        List<HistoricalCharacterDTO> result = characterService.findAll();

        assertEquals(1, result.size());
        assertEquals(testCharacter.getName(), result.get(0).getName());
        verify(repository, times(1)).findAll();
    }

    @Test
    void findById_WhenExists_ShouldReturnCharacter() {
        when(repository.findById(testCharacter.getId())).thenReturn(Optional.of(testCharacter));

        Optional<HistoricalCharacterDTO> result = characterService.findById(testCharacter.getId());

        assertTrue(result.isPresent());
        assertEquals(testCharacter.getName(), result.get().getName());
    }

    @Test
    void findById_WhenNotExists_ShouldReturnEmpty() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<HistoricalCharacterDTO> result = characterService.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void findByName_WhenExists_ShouldReturnCharacter() {
        when(repository.findByName("Mikołaj Kopernik")).thenReturn(Optional.of(testCharacter));

        Optional<HistoricalCharacterDTO> result = characterService.findByName("Mikołaj Kopernik");

        assertTrue(result.isPresent());
        assertEquals("Mikołaj Kopernik", result.get().getName());
    }

    @Test
    void search_ShouldReturnMatchingCharacters() {
        when(repository.searchByName("Kopernik")).thenReturn(Arrays.asList(testCharacter));

        List<HistoricalCharacterDTO> result = characterService.search("Kopernik");

        assertEquals(1, result.size());
        assertTrue(result.get(0).getName().contains("Kopernik"));
    }

    @Test
    void save_ShouldPersistCharacter() {
        when(repository.save(any(HistoricalCharacter.class))).thenReturn(testCharacter);

        HistoricalCharacterDTO result = characterService.save(testDTO);

        assertNotNull(result);
        assertEquals(testCharacter.getName(), result.getName());
        verify(repository, times(1)).save(any(HistoricalCharacter.class));
    }

    @Test
    void update_WhenExists_ShouldUpdateCharacter() {
        when(repository.findById(testCharacter.getId())).thenReturn(Optional.of(testCharacter));
        when(repository.save(any(HistoricalCharacter.class))).thenReturn(testCharacter);

        testDTO.setName("Mikołaj Kopernik (zaktualizowany)");
        HistoricalCharacterDTO result = characterService.update(testCharacter.getId(), testDTO);

        assertNotNull(result);
        verify(repository, times(1)).save(any(HistoricalCharacter.class));
    }

    @Test
    void update_WhenNotExists_ShouldThrowException() {
        when(repository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> 
            characterService.update("nonexistent", testDTO));
    }

    @Test
    void deleteById_WhenExists_ShouldDelete() {
        when(repository.existsById(testCharacter.getId())).thenReturn(true);
        doNothing().when(repository).deleteById(testCharacter.getId());

        assertDoesNotThrow(() -> characterService.deleteById(testCharacter.getId()));
        verify(repository, times(1)).deleteById(testCharacter.getId());
    }

    @Test
    void deleteById_WhenNotExists_ShouldThrowException() {
        when(repository.existsById("nonexistent")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            characterService.deleteById("nonexistent"));
    }
}
