package com.historyai.repository;

import com.historyai.entity.HistoricalCharacter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoricalCharacterRepository extends JpaRepository<HistoricalCharacter, UUID> {

    Optional<HistoricalCharacter> findByName(String name);

    List<HistoricalCharacter> findByEra(String era);

    List<HistoricalCharacter> findByNationality(String nationality);

    @Query("SELECT c FROM HistoricalCharacter c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<HistoricalCharacter> searchByName(String query);

    boolean existsByName(String name);
}
