package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.menu.dto.ModifierDto;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModifierServiceImpl implements ModifierService {

	private final ModifierGroupRepository groupRepository;
	private final ModifierRepository modifierRepository;
	private final MenuItemRepository menuItemRepository;

	// --- Group Logic ---

	@Override
	@Transactional
	public ModifierDto.ModifierGroupResponse createGroup(ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles) {
		ModifierGroup group = ModifierGroup.builder()
				.name(request.name())
				.description(request.description())
				.minSelect(request.minSelect())
				.maxSelect(request.maxSelect())
				.build();
		return mapToGroupResponse(groupRepository.save(group));
	}

	@Override
	public ModifierDto.ModifierGroupResponse getGroupById(UUID id) {
		return groupRepository.findById(id)
				.map(this::mapToGroupResponse)
				.orElseThrow(() -> new MethodArgumentNotValidException("Modifier group not found"));
	}

	@Override
	public List<ModifierDto.ModifierGroupResponse> getAllGroups() {
		return List.of();
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public ModifierDto.ModifierGroupResponse updateGroup(UUID id, ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles) {
		ModifierGroup group = groupRepository.findById(id)
				.orElseThrow(() -> new MethodArgumentNotValidException("Group not found"));
		group.setName(request.name());
		group.setDescription(request.description());
		group.setMinSelect(request.minSelect());
		group.setMaxSelect(request.maxSelect());
		return mapToGroupResponse(groupRepository.save(group));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void deleteGroup(UUID id, UUID currentUserId, String userRoles) {
		if (!groupRepository.existsById(id)) throw new MethodArgumentNotValidException("Group not found");
		groupRepository.deleteById(id);
	}

	// --- Modifier Logic ---

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public ModifierDto.ModifierResponse addModifier(UUID groupId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles) {
		ModifierGroup group = groupRepository.findById(groupId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Group not found"));

		Modifier modifier = Modifier.builder()
				.group(group)
				.name(request.name())
				.description(request.description())
				.priceAdjustment(request.priceAdjustment())
				.build();

		return mapToModifierResponse(modifierRepository.save(modifier));
	}

	@Override
	public ModifierDto.ModifierResponse updateModifier(UUID modifierId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles) {
		return null;
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void deleteModifier(UUID modifierId, UUID currentUserId, String userRoles) {
		if (!modifierRepository.existsById(modifierId)) throw new MethodArgumentNotValidException("Modifier not found");
		modifierRepository.deleteById(modifierId);
	}

	// --- Private Mappers (Manual for Records) ---

	private ModifierDto.ModifierGroupResponse mapToGroupResponse(ModifierGroup group) {
		return new ModifierDto.ModifierGroupResponse(
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
