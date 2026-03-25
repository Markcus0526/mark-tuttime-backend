package com.tuttime.menu.service;

import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.exception.ResourceNotFoundException;
import com.tuttime.menu.mapper.MenuMapper;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuItemService {

	private final MenuItemRepository menuItemRepo;
	private final MenuCategoryRepository categoryRepo;

	public MenuItemDto create(MenuItemDto dto) {
		MenuCategory category = categoryRepo.findById(dto.getCategoryId())
				.orElseThrow(() -> new ResourceNotFoundException("MenuCategory not found with id: " + dto.getCategoryId()));

		MenuItem entity = MenuMapper.toEntity(dto, category);
		MenuItem saved = menuItemRepo.save(entity);
		return MenuMapper.toDto(saved);
	}

	public MenuItemDto getById(Long id) {
		MenuItem item = menuItemRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("MenuItem not found with id: " + id));
		return MenuMapper.toDto(item);
	}

	public List<MenuItemDto> getByCategoryId(Long categoryId) {
		return menuItemRepo.findByCategoryId(categoryId)
				.stream()
				.map(MenuMapper::toDto)
				.collect(Collectors.toList());
	}

	public MenuItemDto update(Long id, MenuItemDto dto) {
		MenuItem existing = menuItemRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("MenuItem not found with id: " + id));
		MenuCategory category = categoryRepo.findById(dto.getCategoryId())
				.orElseThrow(() -> new ResourceNotFoundException("MenuCategory not found with id: " + dto.getCategoryId()));

		existing.setCategory(category);
		existing.setName(dto.getName());
		existing.setDescription(dto.getDescription());
		existing.setPrice(dto.getPrice());
		existing.setIsAvailable(dto.getIsAvailable());

		MenuItem updated = menuItemRepo.save(existing);
		return MenuMapper.toDto(updated);
	}

	public void delete(Long id) {
		if (!menuItemRepo.existsById(id)) {
			throw new ResourceNotFoundException("MenuItem not found with id: " + id);
		}
		menuItemRepo.deleteById(id);
	}
}