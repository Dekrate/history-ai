package com.historyai.controller;

import com.historyai.dto.HistoricalCharacterDTO;
import com.historyai.exception.CharacterAlreadyExistsException;
import com.historyai.exception.CharacterNotFoundException;
import com.historyai.service.CharacterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CharacterController.
 */
@ExtendWith(MockitoExtension.class)
class CharacterControllerTest {

    @Mock
    private CharacterService characterService;

    @InjectMocks
    private CharacterController controller;

    private HistoricalCharacterDTO testDto;
    private UUID testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID();
        testDto = new HistoricalCharacterDTO();
        testDto.setId(testId);
        testDto.setName("Mikołaj Kopernik");
        testDto.setBirthYear(1473);
        testDto.setDeathYear(1543);
        testDto.setBiography("Polish astronomer");
        testDto.setEra("Renaissance");
        testDto.setNationality("Polish");
    }

    @Test
    void getAllCharacters_ShouldReturnAllCharacters() {
        List<HistoricalCharacterDTO> characters = Arrays.asList(testDto);
        when(characterService.findAll()).thenReturn(characters);

        ResponseEntity<List<HistoricalCharacterDTO>> response = controller.getAllCharacters();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().size() == 1);
        assertEquals("Mikołaj Kopernik", response.getBody().get(0).getName());
        verify(characterService, times(1)).findAll();
    }

    @Test
    void getAllCharacters_WhenEmpty_ShouldReturnEmptyList() {
        when(characterService.findAll()).thenReturn(List.of());

        ResponseEntity<List<HistoricalCharacterDTO>> response = controller.getAllCharacters();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    void getCharacterById_WhenExists_ShouldReturnCharacter() {
        when(characterService.findById(testId)).thenReturn(Optional.of(testDto));

        ResponseEntity<HistoricalCharacterDTO> response = controller.getCharacterById(testId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testDto, response.getBody());
    }

    @Test
    void getCharacterById_WhenNotExists_ShouldReturn404() {
        when(characterService.findById(testId)).thenReturn(Optional.empty());

        ResponseEntity<HistoricalCharacterDTO> response = controller.getCharacterById(testId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void searchCharacters_ShouldReturnMatchingCharacters() {
        List<HistoricalCharacterDTO> characters = Arrays.asList(testDto);
        when(characterService.search("Kopernik")).thenReturn(characters);

        ResponseEntity<List<HistoricalCharacterDTO>> response = controller.searchCharacters("Kopernik");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getCharactersByEra_ShouldReturnMatchingCharacters() {
        List<HistoricalCharacterDTO> characters = Arrays.asList(testDto);
        when(characterService.findByEra("Renaissance")).thenReturn(characters);

        ResponseEntity<List<HistoricalCharacterDTO>> response = controller.getCharactersByEra("Renaissance");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals("Renaissance", response.getBody().get(0).getEra());
    }

    @Test
    void getCharactersByNationality_ShouldReturnMatchingCharacters() {
        List<HistoricalCharacterDTO> characters = Arrays.asList(testDto);
        when(characterService.findByNationality("Polish")).thenReturn(characters);

        ResponseEntity<List<HistoricalCharacterDTO>> response = controller.getCharactersByNationality("Polish");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createCharacter_ShouldReturn201() {
        when(characterService.save(any(HistoricalCharacterDTO.class))).thenReturn(testDto);

        ResponseEntity<HistoricalCharacterDTO> response = controller.createCharacter(testDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mikołaj Kopernik", response.getBody().getName());
    }

    @Test
    void createCharacter_WhenAlreadyExists_ShouldThrowException() {
        when(characterService.save(any(HistoricalCharacterDTO.class)))
                .thenThrow(new CharacterAlreadyExistsException("Mikołaj Kopernik"));

        assertThrows(CharacterAlreadyExistsException.class, 
                () -> controller.createCharacter(testDto));
    }

    @Test
    void updateCharacter_ShouldReturn200() {
        when(characterService.update(eq(testId), any(HistoricalCharacterDTO.class))).thenReturn(testDto);

        ResponseEntity<HistoricalCharacterDTO> response = controller.updateCharacter(testId, testDto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testDto, response.getBody());
    }

    @Test
    void updateCharacter_WhenNotExists_ShouldThrowException() {
        when(characterService.update(eq(testId), any(HistoricalCharacterDTO.class)))
                .thenThrow(new CharacterNotFoundException(testId));

        assertThrows(CharacterNotFoundException.class, 
                () -> controller.updateCharacter(testId, testDto));
    }

    @Test
    void deleteCharacter_ShouldReturn204() {
        doNothing().when(characterService).deleteById(testId);

        ResponseEntity<Void> response = controller.deleteCharacter(testId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(characterService, times(1)).deleteById(testId);
    }

    @Test
    void deleteCharacter_WhenNotExists_ShouldThrowException() {
        doThrow(new CharacterNotFoundException(testId)).when(characterService).deleteById(testId);

        assertThrows(CharacterNotFoundException.class, 
                () -> controller.deleteCharacter(testId));
    }

}
