package com.historyai.dto;

import com.historyai.entity.HistoricalCharacter;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

public class HistoricalCharacterDTO {

    private UUID id;
    
    @NotBlank
    private String name;
    private Integer birthYear;
    private Integer deathYear;
    private String biography;
    private String imageUrl;
    private String era;
    private String nationality;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HistoricalCharacterDTO() {
    }

    public static HistoricalCharacterDTO fromEntity(HistoricalCharacter entity) {
        HistoricalCharacterDTO dto = new HistoricalCharacterDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setBirthYear(entity.getBirthYear());
        dto.setDeathYear(entity.getDeathYear());
        dto.setBiography(entity.getBiography());
        dto.setImageUrl(entity.getImageUrl());
        dto.setEra(entity.getEra());
        dto.setNationality(entity.getNationality());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public HistoricalCharacter toEntity() {
        HistoricalCharacter entity = new HistoricalCharacter();
        entity.setName(this.name);
        entity.setBirthYear(this.birthYear);
        entity.setDeathYear(this.deathYear);
        entity.setBiography(this.biography);
        entity.setImageUrl(this.imageUrl);
        entity.setEra(this.era);
        entity.setNationality(this.nationality);
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Integer getDeathYear() {
        return deathYear;
    }

    public void setDeathYear(Integer deathYear) {
        this.deathYear = deathYear;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEra() {
        return era;
    }

    public void setEra(String era) {
        this.era = era;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
