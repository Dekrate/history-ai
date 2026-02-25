package com.historyai.service;

import com.historyai.dto.HistoricalCharacterDTO;
import com.historyai.entity.HistoricalCharacter;
import com.historyai.repository.HistoricalCharacterRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CharacterService {

	private final HistoricalCharacterRepository repository;

	public CharacterService(HistoricalCharacterRepository repository) {
		this.repository = repository;
	}

	public List<HistoricalCharacterDTO> findAll() {
		return repository.findAll()
				.stream()
				.map(HistoricalCharacterDTO::fromEntity)
				.toList();
	}

	public Optional<HistoricalCharacterDTO> findById(UUID id) {
		return repository.findById(id)
				.map(HistoricalCharacterDTO::fromEntity);
	}

	public Optional<HistoricalCharacterDTO> findByName(String name) {
		return repository.findByName(name)
				.map(HistoricalCharacterDTO::fromEntity);
	}

	public List<HistoricalCharacterDTO> findByEra(String era) {
		return repository.findByEra(era)
				.stream()
				.map(HistoricalCharacterDTO::fromEntity)
				.toList();
	}

	public List<HistoricalCharacterDTO> findByNationality(String nationality) {
		return repository.findByNationality(nationality)
				.stream()
				.map(HistoricalCharacterDTO::fromEntity)
				.toList();
	}

	public List<HistoricalCharacterDTO> search(String query) {
		return repository.searchByName(query)
				.stream()
				.map(HistoricalCharacterDTO::fromEntity)
				.toList();
	}

	@Transactional
	public HistoricalCharacterDTO save(HistoricalCharacterDTO dto) {
		if (dto.getBirthYear() != null && dto.getDeathYear() != null 
				&& dto.getBirthYear() > dto.getDeathYear()) {
			throw new IllegalArgumentException("Birth year cannot be after death year");
		}
		
		if (repository.existsByName(dto.getName())) {
			throw new IllegalArgumentException("Character with this name already exists: " + dto.getName());
		}
		
		HistoricalCharacter entity = dto.toEntity();
		HistoricalCharacter saved = repository.save(entity);
		return HistoricalCharacterDTO.fromEntity(saved);
	}

	@Transactional
	public HistoricalCharacterDTO update(UUID id, HistoricalCharacterDTO dto) {
		HistoricalCharacter existing = repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));

		if (dto.getBirthYear() != null && dto.getDeathYear() != null 
				&& dto.getBirthYear() > dto.getDeathYear()) {
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
		return HistoricalCharacterDTO.fromEntity(updated);
	}

	@Transactional
	public void deleteById(UUID id) {
		if (!repository.existsById(id)) {
			throw new IllegalArgumentException("Character not found: " + id);
		}
		repository.deleteById(id);
	}

}
