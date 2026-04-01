package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.dto.ModifierGroupDto;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.Modifier;
import com.tuttime.menu.entity.ModifierGroup;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.ModifierGroupRepository;
import com.tuttime.menu.repository.ModifierRepository;
import com.tuttime.menu.service.ModifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModifierServiceImpl implements ModifierService {

	private final ModifierGroupRepository groupRepository;
	private final ModifierRepository modifierRepository;
	private final MenuItemRepository menuItemRepository;

	// --- Group Logic ---

	@Override
	@Transactional
	public ModifierGroupDto.ModifierGroupResponse createGroup(ModifierGroupDto.ModifierGroupRequest request) {
		ModifierGroup group = ModifierGroup.builder()
				.name(request.name())
				.description(request.description())
				.minSelect(request.minSelect())
				.maxSelect(request.maxSelect())
				.build();
		return mapToGroupResponse(groupRepository.save(group));
	}

	@Override
	@Transactional(readOnly = true)
	public ModifierGroupDto.ModifierGroupResponse getGroupById(UUID id) {
		return groupRepository.findById(id)
				.map(this::mapToGroupResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Modifier group not found"));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void updateGroup(UUID id, ModifierGroupDto.ModifierGroupRequest request) {
		ModifierGroup group = groupRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Group not found"));
		group.setName(request.name());
		group.setDescription(request.description());
		group.setMinSelect(request.minSelect());
		group.setMaxSelect(request.maxSelect());
		groupRepository.save(group);
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void deleteGroup(UUID id) {
		if (!groupRepository.existsById(id)) throw new ResourceNotFoundException("Group not found");
		groupRepository.deleteById(id);
	}

	// --- Modifier Logic ---

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public ModifierDto.ModifierResponse addModifierToGroup(UUID groupId, ModifierDto.ModifierRequest request) {
		ModifierGroup group = groupRepository.findById(groupId)
				.orElseThrow(() -> new ResourceNotFoundException("Group not found"));

		Modifier modifier = Modifier.builder()
				.group(group)
				.name(request.name())
				.description(request.description())
				.priceAdjustment(request.priceAdjustment())
				.build();

		return mapToModifierResponse(modifierRepository.save(modifier));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void deleteModifier(UUID modifierId) {
		if (!modifierRepository.existsById(modifierId)) throw new ResourceNotFoundException("Modifier not found");
		modifierRepository.deleteById(modifierId);
	}

	// --- Linking Logic (The Many-to-Many Bridge) ---

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void assignGroupToItem(UUID itemId, UUID groupId) {
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new ResourceNotFoundException("Menu Item not found"));
		ModifierGroup group = groupRepository.findById(groupId)
				.orElseThrow(() -> new ResourceNotFoundException("Modifier Group not found"));

		// Since MenuItem has a Set<ModifierGroup>, we just add it
		item.getModifierGroups().add(group);
		menuItemRepository.save(item);
	}

	// --- Private Mappers (Manual for Records) ---

	private ModifierGroupDto.ModifierGroupResponse mapToGroupResponse(ModifierGroup group) {
		return new ModifierGroupDto.ModifierGroupResponse(
				group.getId(),
				group.getName(),
				group.getDescription(),
				group.getMinSelect(),
				group.getMaxSelect(),
				group.getModifiers() != null ? group.getModifiers().stream()
						.map(this::mapToModifierResponse)
						.collect(Collectors.toList()) : null
		);
	}

	private ModifierDto.ModifierResponse mapToModifierResponse(Modifier m) {
		return new ModifierDto.ModifierResponse(
				m.getId(),
				m.getName(),
				m.getDescription(),
				m.getPriceAdjustment()
		);
	}
}
