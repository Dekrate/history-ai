package com.historyai.service;

import com.historyai.dto.HistoricalCharacterDTO;
import com.historyai.entity.HistoricalCharacter;
import com.historyai.repository.HistoricalCharacterRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
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

	public Optional<HistoricalCharacterDTO> findById(String id) {
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

	public HistoricalCharacterDTO save(HistoricalCharacterDTO dto) {
		HistoricalCharacter entity = dto.toEntity();
		HistoricalCharacter saved = repository.save(entity);
		return HistoricalCharacterDTO.fromEntity(saved);
	}

	public HistoricalCharacterDTO update(String id, HistoricalCharacterDTO dto) {
		HistoricalCharacter existing = repository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Character not found: " + id));

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

	public void deleteById(String id) {
		if (!repository.existsById(id)) {
			throw new IllegalArgumentException("Character not found: " + id);
		}
		repository.deleteById(id);
	}

}
