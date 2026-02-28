package com.historyai.service;

import com.historyai.dto.HistoricalCharacterDTO;
import com.historyai.entity.HistoricalCharacter;
import com.historyai.exception.CharacterAlreadyExistsException;
import com.historyai.exception.CharacterNotFoundException;
import com.historyai.exception.CharacterNotFoundInWikipediaException;
import com.historyai.repository.HistoricalCharacterRepository;
import com.historyai.dto.WikipediaResponse;
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
    private final WikipediaService wikipediaService;

    public CharacterService(final HistoricalCharacterRepository repository, WikipediaService wikipediaService) {
        this.repository = repository;
        this.wikipediaService = wikipediaService;
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
    public Optional<HistoricalCharacterDTO> findById(final UUID id) {
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
    public Optional<HistoricalCharacterDTO> findByName(final String name) {
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

        String newName = dto.getName();
        if (newName != null && !newName.equals(existing.getName())
                && repository.existsByName(newName)) {
            logger.warn("Attempt to update character to an existing name: {}", newName);
            throw new CharacterAlreadyExistsException(newName);
        }

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

    @Transactional
    public HistoricalCharacterDTO searchAndImportFromWikipedia(String searchQuery) {
        logger.info("Searching and importing character from Wikipedia: {}", searchQuery);
        
        WikipediaResponse wikiResponse;
        try {
            wikiResponse = wikipediaService.getCharacterInfo(searchQuery);
        } catch (CharacterNotFoundInWikipediaException e) {
            logger.warn("Character not found in Wikipedia: {}", searchQuery);
            throw e;
        }
        
        if (repository.existsByName(wikiResponse.title())) {
            logger.info("Character already exists: {}", wikiResponse.title());
            return repository.findByName(wikiResponse.title())
                    .map(HistoricalCharacterDTO::fromEntity)
                    .orElseThrow();
        }
        
        HistoricalCharacter character = new HistoricalCharacter();
        character.setName(wikiResponse.title());
        character.setBiography(wikiResponse.extract() != null ? wikiResponse.extract() : "");
        character.setImageUrl(wikiResponse.thumbnail() != null ? wikiResponse.thumbnail().source() : null);
        character.setEra(extractEraFromWiki(wikiResponse.extract()));
        String nationality = "Unknown";
        if (wikiResponse.wikibaseItem() != null && !wikiResponse.wikibaseItem().isBlank()) {
            nationality = wikipediaService.getNationalityFromWikidata(wikiResponse.wikibaseItem());
        }
        if ("Unknown".equals(nationality)) {
            nationality = extractNationalityFromWiki(wikiResponse.extract());
        }
        character.setNationality(nationality);
        
        HistoricalCharacter saved = repository.save(character);
        logger.info("Character imported from Wikipedia: {}", saved.getId());
        return HistoricalCharacterDTO.fromEntity(saved);
    }

    private String extractEraFromWiki(String extract) {
        if (extract == null) return "Unknown";
        extract = extract.toLowerCase();
        
        if (extract.contains("xix wiek") || extract.contains("19th century") || extract.contains("180") || extract.contains("186")) {
            return "XIX wiek";
        } else if (extract.contains("xx wiek") || extract.contains("20th century") || extract.contains("187") || extract.contains("188") || extract.contains("189") || extract.contains("190") || extract.contains("191") || extract.contains("192") || extract.contains("193") || extract.contains("194") || extract.contains("195") || extract.contains("196") || extract.contains("197") || extract.contains("198") || extract.contains("199")) {
            return "XX wiek";
        } else if (extract.contains("xviii wiek") || extract.contains("18th century") || extract.contains("17th century") || extract.contains("170") || extract.contains("171") || extract.contains("172") || extract.contains("173") || extract.contains("174") || extract.contains("175") || extract.contains("176") || extract.contains("177") || extract.contains("178") || extract.contains("179")) {
            return "XVIII wiek";
        } else if (extract.contains("xxi wiek") || extract.contains("21st century") || extract.contains("2000")) {
            return "XXI wiek";
        } else if (extract.contains("średniowiecze") || extract.contains("średniowieczny") || extract.contains("middle ages") || extract.contains("medieval")) {
            return "Średniowiecze";
        } else if (extract.contains("renesans") || extract.contains("renaissance")) {
            return "Renesans";
        } else if (extract.contains("born") && extract.contains("19")) {
            return "XIX wiek";
        } else if (extract.contains("born") && extract.contains("20")) {
            return "XX wiek";
        } else if (extract.contains("born") && extract.contains("18")) {
            return "XVIII wiek";
        } else if (extract.contains("urodz") && extract.contains("19")) {
            return "XIX wiek";
        } else if (extract.contains("urodz") && extract.contains("20")) {
            return "XX wiek";
        } else if (extract.contains("urodz") && extract.contains("18")) {
            return "XVIII wiek";
        }
        return "Unknown";
    }

    private String extractNationalityFromWiki(String extract) {
        if (extract == null) return "Unknown";
        extract = extract.toLowerCase();
        if (extract.contains("polish") || extract.contains("poland") || extract.contains("polski") || extract.contains("polska") || extract.contains("polscy") || extract.contains("polskie")) {
            return "Polska";
        } else if (extract.contains("french") || extract.contains("france") || extract.contains("francuski") || extract.contains("francja")) {
            return "Francja";
        } else if (extract.contains("german-born") || extract.contains("german ") || extract.contains("germany") || extract.contains("niemiecki") || extract.contains("niemcy") || extract.contains("niemiecka")) {
            return "Niemcy";
        } else if (extract.contains("british") || extract.contains("england") || extract.contains("united kingdom") || extract.contains("brytyjski") || extract.contains("angielski") || extract.contains("anglia") || extract.contains("wielka brytania") || extract.contains("zjednoczone królestwo")) {
            return "Wielka Brytania";
        } else if (extract.contains("american") || extract.contains("united states") || extract.contains("u.s.") || extract.contains("amerykański") || extract.contains("amerykanski") || extract.contains("stany zjednoczone") || extract.contains("usa")) {
            return "USA";
        } else if (extract.contains("italian") || extract.contains("italy") || extract.contains("włoski") || extract.contains("wloski") || extract.contains("włochy") || extract.contains("wlochy")) {
            return "Włochy";
        } else if (extract.contains("russian") || extract.contains("russia") || extract.contains("rosyjski") || extract.contains("rosja")) {
            return "Rosja";
        } else if (extract.contains("spanish") || extract.contains("spain") || extract.contains("hiszpański") || extract.contains("hiszpanski") || extract.contains("hiszpania")) {
            return "Hiszpania";
        }
        return "Unknown";
    }

}
