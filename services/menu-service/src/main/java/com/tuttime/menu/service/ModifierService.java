package com.tuttime.menu.service;

import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.entity.Modifier;
import com.tuttime.menu.exception.ResourceNotFoundException;
import com.tuttime.menu.mapper.MenuMapper;
import com.tuttime.menu.repository.ModifierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModifierService {
	private final ModifierRepository repository;

	public ModifierDto create(ModifierDto dto) {
		Modifier entity = MenuMapper.toEntity(dto);
		Modifier saved = repository.save(entity);
		return MenuMapper.toDto(saved);
	}

	public ModifierDto getById(Long id) {
		Modifier modifier = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Modifier not found with id: " + id));
		return MenuMapper.toDto(modifier);
	}

	public List<ModifierDto> getAll() {
		return repository.findAll()
				.stream()
				.map(MenuMapper::toDto)
				.collect(Collectors.toList());
	}

	public ModifierDto update(Long id, ModifierDto dto) {
		Modifier existing = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Modifier not found with id: " + id));

		existing.setName(dto.getName());
		existing.setDescription(dto.getDescription());
		existing.setPriceAdjustment(dto.getPriceAdjustment());

		Modifier updated = repository.save(existing);
		return MenuMapper.toDto(updated);
	}

	public void delete(Long id) {
		if (!repository.existsById(id)) {
			throw new ResourceNotFoundException("Modifier not found with id: " + id);
		}
		repository.deleteById(id);
	}
}