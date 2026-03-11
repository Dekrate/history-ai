package com.historyai.controller;

import com.historyai.dto.HistoricalCharacterDTO;
import com.historyai.service.CharacterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing historical characters.
 *
 * <p>Provides endpoints for:</p>
 * <ul>
 *   <li>Listing all characters</li>
 *   <li>Finding characters by ID, name, era, or nationality</li>
 *   <li>Searching characters by name</li>
 *   <li>Creating, updating, and deleting characters</li>
 *   <li>Importing characters from Wikipedia</li>
 * </ul>
 *
 * <p>All endpoints return appropriate HTTP status codes and follow REST conventions.</p>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see HistoricalCharacterDTO
 * @see CharacterService
 * @see io.swagger.v3.oas.annotations.tags.Tag
 */
@RestController
@RequestMapping("/api/characters")
@Validated
@Tag(name = "Characters", description = "Historical character management API")
public class CharacterController {

    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @GetMapping
    public ResponseEntity<List<HistoricalCharacterDTO>> getAllCharacters() {
        return ResponseEntity.ok(characterService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistoricalCharacterDTO> getCharacterById(@PathVariable UUID id) {
        return characterService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<HistoricalCharacterDTO>> searchCharacters(
            @RequestParam @NotBlank String q) {
        return ResponseEntity.ok(characterService.search(q));
    }

    @GetMapping("/era/{era}")
    public ResponseEntity<List<HistoricalCharacterDTO>> getCharactersByEra(@PathVariable String era) {
        return ResponseEntity.ok(characterService.findByEra(era));
    }

    @GetMapping("/nationality/{nationality}")
    public ResponseEntity<List<HistoricalCharacterDTO>> getCharactersByNationality(@PathVariable String nationality) {
        return ResponseEntity.ok(characterService.findByNationality(nationality));
    }

    @PostMapping
    public ResponseEntity<HistoricalCharacterDTO> createCharacter(@Valid @RequestBody HistoricalCharacterDTO dto) {
        HistoricalCharacterDTO saved = characterService.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HistoricalCharacterDTO> updateCharacter(
            @PathVariable UUID id,
            @Valid @RequestBody HistoricalCharacterDTO dto) {
        HistoricalCharacterDTO updated = characterService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCharacter(@PathVariable UUID id) {
        characterService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/import")
    public ResponseEntity<HistoricalCharacterDTO> importFromWikipedia(@RequestParam @NotBlank String name) {
        HistoricalCharacterDTO imported = characterService.searchAndImportFromWikipedia(name);
        return ResponseEntity.status(HttpStatus.CREATED).body(imported);
    }
}
