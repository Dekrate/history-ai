package com.historyai.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA entity representing a historical character in the HistoryAI system.
 *
 * <p>This entity stores information about historical figures that users can chat with.
 * Each character has biographical information including name, birth/death years,
 * nationality, era, and a biography. The entity uses time-ordered UUIDs for
 * primary key generation and supports automatic auditing of creation/modification timestamps.</p>
 *
 * <p>The entity is mapped to the {@code historical_characters} database table
 * and is managed by JPA repositories in the application.</p>
 *
 * @author HistoryAI Team
 * @version 1.0
 * @since 1.0
 * @see com.historyai.repository.HistoricalCharacterRepository
 * @see com.historyai.dto.HistoricalCharacterDTO
 */
@Entity
@Table(name = "historical_characters")
@EntityListeners(AuditingEntityListener.class)
public class HistoricalCharacter {

    /**
     * Unique identifier for the historical character.
     * Uses time-ordered UUID for better database performance.
     */
    @Id
    private UUID id;

    /**
     * Full name of the historical character.
     * This field is required and cannot be null.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Year of birth for the historical character.
     * Can be null for characters with unknown birth year.
     */
    @Column(name = "birth_year")
    private Integer birthYear;

    /**
     * Year of death for the historical character.
     * Can be null for still-living figures (though unlikely for historical characters)
     * or for characters with unknown death year.
     */
    @Column(name = "death_year")
    private Integer deathYear;

    /**
     * Biographical information about the historical character.
     * Contains a detailed biography or description of the person's life and achievements.
     */
    @Column(columnDefinition = "TEXT")
    private String biography;

    /**
     * URL to an image of the historical character.
     * Stored as a string up to 512 characters, can be null if no image is available.
     */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /**
     * Historical era to which this character belongs (e.g., "Renaissance", "XIX wiek").
     * Helps categorize characters for filtering and display purposes.
     */
    @Column(length = 100)
    private String era;

    /**
     * Nationality of the historical character (e.g., "Polska", "Francja").
     * Used for filtering and display purposes.
     */
    @Column(length = 100)
    private String nationality;

    /**
     * Timestamp when this entity was first created.
     * Automatically managed by Spring Data JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this entity was last modified.
     * Automatically managed by Spring Data JPA auditing.
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor required by JPA.
     * Should not be used directly - use the parameterized constructor or
     * let JPA create instances from the database.
     */
    public HistoricalCharacter() {
    }

    /**
     * Constructs a new HistoricalCharacter with the specified basic information.
     *
     * @param name        the full name of the historical character (required)
     * @param biography   the biographical information about the character
     * @param era         the historical era to which the character belongs
     * @param nationality the nationality of the character
     */
    public HistoricalCharacter(String name, String biography, String era, String nationality) {
        this.name = name;
        this.biography = biography;
        this.era = era;
        this.nationality = nationality;
    }

    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this historical character.
     * This method is typically called by JPA when loading from the database.
     * For new entities, the ID is automatically generated using time-ordered UUID
     * via the {@link #prePersist()} method.
     *
     * @param id the UUID to set as the entity's identifier
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Generates a time-ordered UUID before persisting the entity
     * if no ID has been explicitly set.
     * This ensures consistent ID generation across distributed systems.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrdered();
        }
    }

    /**
     * Returns the name of the historical character.
     *
     * @return the character's full name, or null if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the historical character.
     *
     * @param name the full name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the birth year of the historical character.
     *
     * @return the birth year, or null if unknown
     */
    public Integer getBirthYear() {
        return birthYear;
    }

    /**
     * Sets the birth year of the historical character.
     *
     * @param birthYear the year of birth to set
     */
    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    /**
     * Returns the death year of the historical character.
     *
     * @return the death year, or null if unknown or still alive
     */
    public Integer getDeathYear() {
        return deathYear;
    }

    /**
     * Sets the death year of the historical character.
     *
     * @param deathYear the year of death to set
     */
    public void setDeathYear(Integer deathYear) {
        this.deathYear = deathYear;
    }

    /**
     * Returns the biography of the historical character.
     *
     * @return the biographical information, or null if not set
     */
    public String getBiography() {
        return biography;
    }

    /**
     * Sets the biography of the historical character.
     *
     * @param biography the biographical text to set
     */
    public void setBiography(String biography) {
        this.biography = biography;
    }

    /**
     * Returns the URL to the character's image.
     *
     * @return the image URL, or null if not available
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the URL to the character's image.
     *
     * @param imageUrl the image URL to set (max 512 characters)
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Returns the historical era of the character.
     *
     * @return the era (e.g., "Renaissance", "XIX wiek"), or null if not set
     */
    public String getEra() {
        return era;
    }

    /**
     * Sets the historical era of the character.
     *
     * @param era the era to set
     */
    public void setEra(String era) {
        this.era = era;
    }

    /**
     * Returns the nationality of the historical character.
     *
     * @return the nationality (e.g., "Polska", "Francja"), or null if not set
     */
    public String getNationality() {
        return nationality;
    }

    /**
     * Sets the nationality of the historical character.
     *
     * @param nationality the nationality to set
     */
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    /**
     * Returns the timestamp when this entity was created.
     *
     * @return the creation timestamp, or null if not yet persisted
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp.
     * Typically managed automatically by Spring Data JPA auditing.
     *
     * @param createdAt the timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns the timestamp when this entity was last modified.
     *
     * @return the last modification timestamp, or null if not yet persisted
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last modification timestamp.
     * Typically managed automatically by Spring Data JPA auditing.
     *
     * @param updatedAt the timestamp to set
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
