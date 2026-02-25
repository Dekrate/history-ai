package com.historyai.service;

import com.historyai.dto.HistoricalCharacterDTO;
import com.historyai.entity.HistoricalCharacter;
import com.historyai.exception.CharacterAlreadyExistsException;
import com.historyai.exception.CharacterNotFoundException;
import com.historyai.repository.HistoricalCharacterRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for managing historical characters.
 * Provides business logic for CRUD operations and search functionality.
 */
@Service
@Transactional(readOnly = true)
public class CharacterService {

    private static final Logger logger = LoggerFactory.getLogger(CharacterService.class);

    private final HistoricalCharacterRepository repository;

    /**
     * Constructs a new CharacterService with the given repository.
     *
     * @param repository the character repository
     */
    public CharacterService(HistoricalCharacterRepository repository) {
        this.repository = repository;
    }

    /**
     * Retrieves all historical characters.
     *
     * @return list of all characters as DTOs
     */
    public List<HistoricalCharacterDTO> findAll() {
        logger.debug("Finding all characters");
        return repository.findAll()
                .stream()
                .map(HistoricalCharacterDTO::fromEntity)
                .toList();
    }

    /**
     * Finds a character by its unique identifier.
     *
     * @param id the character UUID
     * @return optional containing the character DTO if found
     */
    public Optional<HistoricalCharacterDTO> findById(UUID id) {
        logger.debug("Finding character by id: {}", id);
        return repository.findById(id)
                .map(HistoricalCharacterDTO::fromEntity);
    }

    /**
     * Finds a character by name.
     *
     * @param name the character name
     * @return optional containing the character DTO if found
     */
    public Optional<HistoricalCharacterDTO> findByName(String name) {
        logger.debug("Finding character by name: {}", name);
        return repository.findByName(name)
                .map(HistoricalCharacterDTO::fromEntity);
    }

    /**
     * Finds all characters from a specific era.
     *
     * @param era the historical era
     * @return list of matching characters as DTOs
     */
    public List<HistoricalCharacterDTO> findByEra(String era) {
        logger.debug("Finding characters by era: {}", era);
        return repository.findByEra(era)
                .stream()
                .map(HistoricalCharacterDTO::fromEntity)
                .toList();
    }

    /**
     * Finds all characters of a specific nationality.
     *
     * @param nationality the nationality
     * @return list of matching characters as DTOs
     */
    public List<HistoricalCharacterDTO> findByNationality(String nationality) {
        logger.debug("Finding characters by nationality: {}", nationality);
        return repository.findByNationality(nationality)
                .stream()
                .map(HistoricalCharacterDTO::fromEntity)
                .toList();
    }

    /**
     * Searches characters by name query.
     *
     * @param query the search query
     * @return list of matching characters as DTOs
     */
    public List<HistoricalCharacterDTO> search(String query) {
        logger.debug("Searching characters with query: {}", query);
        return repository.searchByName(query)
                .stream()
                .map(HistoricalCharacterDTO::fromEntity)
                .toList();
    }

    /**
     * Creates a new historical character.
     *
     * @param dto the character data transfer object
     * @return the created character as DTO
     * @throws IllegalArgumentException if birth year is after death year
     * @throws CharacterAlreadyExistsException if character with same name exists
     */
    @Transactional
    public HistoricalCharacterDTO save(HistoricalCharacterDTO dto) {
        logger.info("Creating new character: {}", dto.getName());
        
        if (dto.getBirthYear() != null && dto.getDeathYear() != null 
                && dto.getBirthYear() > dto.getDeathYear()) {
            logger.warn("Invalid date range for character: {} (birth: {}, death: {})", 
                    dto.getName(), dto.getBirthYear(), dto.getDeathYear());
            throw new IllegalArgumentException("Birth year cannot be after death year");
        }
        
        if (repository.existsByName(dto.getName())) {
            logger.warn("Character already exists: {}", dto.getName());
            throw new CharacterAlreadyExistsException(dto.getName());
        }
        
        HistoricalCharacter entity = dto.toEntity();
        HistoricalCharacter saved = repository.save(entity);
        logger.info("Character created with id: {}", saved.getId());
        return HistoricalCharacterDTO.fromEntity(saved);
    }

    /**
     * Updates an existing historical character.
     *
     * @param id  the character UUID
     * @param dto the updated character data
     * @return the updated character as DTO
     * @throws CharacterNotFoundException if character not found
     * @throws IllegalArgumentException if birth year is after death year
     */
    @Transactional
    public HistoricalCharacterDTO update(UUID id, HistoricalCharacterDTO dto) {
        logger.info("Updating character with id: {}", id);
        
        HistoricalCharacter existing = repository.findById(id)
                .orElseThrow(() -> new CharacterNotFoundException(id));

        if (dto.getBirthYear() != null && dto.getDeathYear() != null 
                && dto.getBirthYear() > dto.getDeathYear()) {
            logger.warn("Invalid date range for character update: {} (birth: {}, death: {})", 
                    dto.getName(), dto.getBirthYear(), dto.getDeathYear());
            throw new IllegalArgumentException("Birth year cannot be after death year");
        }

        existing.setName(dto.getName());
        existing.setBirthYear(dto.getBirthYear());
        existing.setDeathYear(dto.getDeathYear());
        existing.setBiography(dto.getBiography());
        existing.setImageUrl(dto.getImageUrl());
        existing.setEra(dto.getEra());
        existing.setNationality(dto.getNationality());

        HistoricalCharacter updated = repository.save(existing);
        logger.info("Character updated: {}", updated.getId());
        return HistoricalCharacterDTO.fromEntity(updated);
    }

    /**
     * Deletes a character by its ID.
     *
     * @param id the character UUID
     * @throws CharacterNotFoundException if character not found
     */
    @Transactional
    public void deleteById(UUID id) {
        logger.info("Deleting character with id: {}", id);
        
        if (!repository.existsById(id)) {
            logger.warn("Character not found for deletion: {}", id);
            throw new CharacterNotFoundException(id);
        }
        
        repository.deleteById(id);
        logger.info("Character deleted: {}", id);
    }

}
