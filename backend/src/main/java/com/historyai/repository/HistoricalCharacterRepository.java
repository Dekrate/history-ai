package com.historyai.repository;

import com.historyai.entity.HistoricalCharacter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository interface for {@link HistoricalCharacter} entities.
 *
 * <p>Provides database access methods for historical character data.
 * Extends {@link JpaRepository} to inherit standard CRUD operations
 * and pagination support.</p>
 *
 * <p>Custom query methods include:</p>
 * <ul>
 *   <li>Find by exact name match</li>
 *   <li>Find by era</li>
 *   <li>Find by nationality</li>
 *   <li>Search by name (case-insensitive partial match)</li>
 *   <li>Check if character exists by name</li>
 * </ul>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @see HistoricalCharacter
 * @see JpaRepository
 */
@Repository
public interface HistoricalCharacterRepository extends JpaRepository<HistoricalCharacter, UUID> {

    /**
     * Finds a historical character by exact name match.
     *
     * @param name the name to search for (case-insensitive via repository implementation)
     * @return an {@link Optional} containing the character if found, or empty if not found
     */
    Optional<HistoricalCharacter> findByName(String name);

    /**
     * Finds all historical characters belonging to a specific era.
     *
     * @param era the era to filter by (e.g., "Renaissance", "XIX wiek")
     * @return a list of characters belonging to the specified era, or empty list if none found
     */
    List<HistoricalCharacter> findByEra(String era);

    /**
     * Finds all historical characters with a specific nationality.
     *
     * @param nationality the nationality to filter by (e.g., "Polska", "Francja")
     * @return a list of characters with the specified nationality, or empty list if none found
     */
    List<HistoricalCharacter> findByNationality(String nationality);

    /**
     * Searches for characters by name using case-insensitive partial matching.
     *
     * <p>The search wraps the query in wildcards to match names containing
     * the search term anywhere in the name.</p>
     *
     * @param query the search term to match against character names
     * @return a list of characters whose names contain the query (case-insensitive)
     */
    @Query("SELECT c FROM HistoricalCharacter c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<HistoricalCharacter> searchByName(@Param("query") String query);

    /**
     * Checks if a character with the given name already exists.
     *
     * @param name the name to check for existence
     * @return true if a character with the given name exists, false otherwise
     */
    boolean existsByName(String name);
}
