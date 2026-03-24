package com.tuttime.menu.service;

import com.tuttime.menu.dto.MenuCategoryDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.exception.ResourceNotFoundException;
import com.tuttime.menu.mapper.MenuMapper;
import com.tuttime.menu.repository.MenuCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuCategoryService {

    private final MenuCategoryRepository repository;

    public MenuCategoryDto create(MenuCategoryDto dto) {
        MenuCategory entity = MenuMapper.toEntity(dto);
        MenuCategory saved = repository.save(entity);
        return MenuMapper.toDto(saved);
    }

    public MenuCategoryDto getById(Long id) {
        MenuCategory category = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory not found with id: " + id));
        return MenuMapper.toDto(category);
    }

    public List<MenuCategoryDto> getByBranchId(Long branchId) {
        return repository.findByBranchId(branchId)
                .stream()
                .map(MenuMapper::toDto)
                .collect(Collectors.toList());
    }

    public MenuCategoryDto update(Long id, MenuCategoryDto dto) {
        MenuCategory existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory not found with id: " + id));
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setBranchId(dto.getBranchId());
        MenuCategory updated = repository.save(existing);
        return MenuMapper.toDto(updated);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("MenuCategory not found with id: " + id);
        }
        repository.deleteById(id);
    }
}