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
 * Entity representing a historical character in the application.
 */
@Entity
@Table(name = "historical_characters")
@EntityListeners(AuditingEntityListener.class)
public class HistoricalCharacter {

    /** Unique identifier of the character. */
    @Id
    private UUID id;

    /** Name of the historical character. */
    @Column(nullable = false)
    private String name;

    /** Year of birth. */
    @Column(name = "birth_year")
    private Integer birthYear;

    /** Year of death. */
    @Column(name = "death_year")
    private Integer deathYear;

    /** Biography or description of the character. */
    @Column(columnDefinition = "TEXT")
    private String biography;

    /** URL to the character's image. */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** Historical era (e.g., Renaissance, Modern). */
    @Column(length = 100)
    private String era;

    /** Nationality of the character. */
    @Column(length = 100)
    private String nationality;

    /** Timestamp when the character was created. */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the character was last updated. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Default constructor required by JPA.
     */
    public HistoricalCharacter() {
    }

    /**
     * Constructs a new HistoricalCharacter with the specified attributes.
     *
     * @param name        the name of the character
     * @param biography   the biography of the character
     * @param era         the historical era
     * @param nationality the nationality of the character
     */
    public HistoricalCharacter(final String name, final String biography,
            final String era, final String nationality) {
        this.name = name;
        this.biography = biography;
        this.era = era;
        this.nationality = nationality;
    }

    /**
     * Gets the unique identifier of the character.
     *
     * @return the unique identifier
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the character.
     *
     * @param id the unique identifier
     */
    public void setId(final UUID id) {
        this.id = id;
    }

    /**
     * Generates a new UUID before persisting the entity.
     */
    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UuidCreator.getTimeOrdered();
        }
    }

    /**
     * Gets the name of the character.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the character.
     *
     * @param name the name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the birth year of the character.
     *
     * @return the birth year
     */
    public Integer getBirthYear() {
        return birthYear;
    }

    /**
     * Sets the birth year of the character.
     *
     * @param birthYear the birth year
     */
    public void setBirthYear(final Integer birthYear) {
        this.birthYear = birthYear;
    }

    /**
     * Gets the death year of the character.
     *
     * @return the death year
     */
    public Integer getDeathYear() {
        return deathYear;
    }

    /**
     * Sets the death year of the character.
     *
     * @param deathYear the death year
     */
    public void setDeathYear(final Integer deathYear) {
        this.deathYear = deathYear;
    }

    /**
     * Gets the biography of the character.
     *
     * @return the biography
     */
    public String getBiography() {
        return biography;
    }

    /**
     * Sets the biography of the character.
     *
     * @param biography the biography
     */
    public void setBiography(final String biography) {
        this.biography = biography;
    }

    /**
     * Gets the image URL of the character.
     *
     * @return the image URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the image URL of the character.
     *
     * @param imageUrl the image URL
     */
    public void setImageUrl(final String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Gets the historical era of the character.
     *
     * @return the era
     */
    public String getEra() {
        return era;
    }

    /**
     * Sets the historical era of the character.
     *
     * @param era the era
     */
    public void setEra(final String era) {
        this.era = era;
    }

    /**
     * Gets the nationality of the character.
     *
     * @return the nationality
     */
    public String getNationality() {
        return nationality;
    }

    /**
     * Sets the nationality of the character.
     *
     * @param nationality the nationality
     */
    public void setNationality(final String nationality) {
        this.nationality = nationality;
    }

    /**
     * Gets the creation timestamp of the character.
     *
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the character.
     *
     * @param createdAt the creation timestamp
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the last update timestamp of the character.
     *
     * @return the last update timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp of the character.
     *
     * @param updatedAt the last update timestamp
     */
    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
