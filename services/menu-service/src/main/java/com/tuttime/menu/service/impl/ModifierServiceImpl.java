package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.Modifier;
import com.tuttime.menu.entity.ModifierGroup;
import com.tuttime.menu.mapper.ModifierMapper;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.ModifierGroupRepository;
import com.tuttime.menu.repository.ModifierRepository;
import com.tuttime.menu.service.ModifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModifierServiceImpl implements ModifierService {

	private final ModifierGroupRepository modifierGroupRepository;
	private final ModifierRepository modifierRepository;
	private final MenuItemRepository menuItemRepository;
	private final ModifierMapper modifierMapper;

	// --- Group Logic ---

	@Override
	@Transactional(readOnly = true)
	public Page<ModifierDto.ModifierGroupResponse> findGroups(String query, UUID restaurantId, Pageable pageable) {
		// Build dynamic specification
		Specification<ModifierGroup> spec = Specification.where(null);

		if (restaurantId != null) {
			spec = spec.and((root, q, cb) -> cb.equal(root.get("restaurantId"), restaurantId));
		}

		if (query != null && !query.isBlank()) {
			spec = spec.and((root, q, cb) ->
					cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%"));
		}

		// Fetch paginated results from repository
		Page<ModifierGroup> groupPage = modifierGroupRepository.findAll(spec, pageable);

		// Map to Response DTOs
		return groupPage.map(modifierMapper::toGroupResponse);
	}

	@Override
	@Transactional
	public ModifierDto.ModifierGroupResponse createGroup(ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles) {
		ModifierGroup group = ModifierGroup.builder()
				.name(request.name())
				.description(request.description())
				.minSelect(request.minSelect())
				.maxSelect(request.maxSelect())
				.build();
		return modifierMapper.toGroupResponse(modifierGroupRepository.save(group));
	}

	@Override
	@Transactional(readOnly = true)
	public ModifierDto.ModifierGroupResponse getGroupById(UUID id) {
		return modifierGroupRepository.findById(id)
				.map(modifierMapper::toGroupResponse)
				.orElseThrow(() -> new InvalidRequestException("Modifier group not found"));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ModifierDto.ModifierGroupResponse> getAllGroups() {
		return List.of();
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public ModifierDto.ModifierGroupResponse updateGroup(UUID id, ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles) {
		ModifierGroup group = modifierGroupRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Group not found"));
		group.setName(request.name());
		group.setDescription(request.description());
		group.setMinSelect(request.minSelect());
		group.setMaxSelect(request.maxSelect());
		return modifierMapper.toGroupResponse(modifierGroupRepository.save(group));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void deleteGroup(UUID id, UUID currentUserId, String userRoles) {
		if (!modifierGroupRepository.existsById(id)) throw new InvalidRequestException("Group not found");
		modifierGroupRepository.deleteById(id);
	}

	// --- Modifier Logic ---

	@Override
	@Transactional(readOnly = true)
	public List<ModifierDto.ModifierResponse> getModifiersByGroupId(UUID groupId) {
		// 1. Check if group exists (optional but recommended for 404 handling)
		if (!modifierGroupRepository.existsById(groupId)) {
			throw new InvalidRequestException("Modifier Group not found with id: " + groupId);
		}

		// 2. Fetch modifiers from repository
		List<Modifier> modifiers = modifierRepository.findByModifierGroupId(groupId);

		// 3. Map to DTOs using your Mapper
		return modifiers.stream()
				.map(modifierMapper::toModifierResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public ModifierDto.ModifierResponse addModifier(UUID groupId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles) {
		ModifierGroup group = modifierGroupRepository.findById(groupId)
				.orElseThrow(() -> new InvalidRequestException("Group not found"));

		Modifier modifier = Modifier.builder()
				.modifierGroup(group)
				.name(request.name())
				.description(request.description())
				.priceAdjustment(request.priceAdjustment())
				.build();

		return modifierMapper.toModifierResponse(modifierRepository.save(modifier));
	}

	@Override
	@Transactional
	public ModifierDto.ModifierResponse updateModifier(UUID modifierId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles) {
		Modifier modifier = modifierRepository.findById(modifierId)
				.orElseThrow(() -> new InvalidRequestException("Modifier not found with id: " + modifierId));

		modifier.setName(request.name());
		modifier.setPriceAdjustment(request.priceAdjustment());

		Modifier updatedModifier = modifierRepository.save(modifier);

		return modifierMapper.toModifierResponse(updatedModifier);
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", allEntries = true)
	public void deleteModifier(UUID modifierId, UUID currentUserId, String userRoles) {
		if (!modifierRepository.existsById(modifierId)) throw new InvalidRequestException("Modifier not found");
		modifierRepository.deleteById(modifierId);
	}

	@Override
	@Transactional
	public void linkToItem(UUID groupId, UUID itemId) {
		// 1. Fetch both entities using your specific repositories
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Menu Item not found"));

		ModifierGroup group = modifierGroupRepository.findById(groupId)
				.orElseThrow(() -> new InvalidRequestException("Modifier Group not found"));

		// 2. Add to the Set (JPA handles the 'menu_item_modifier_groups' table)
		// HashSet prevents duplicate associations automatically
		item.getModifierGroups().add(group);

		// 3. Save the item (the owning side of this relationship)
		menuItemRepository.save(item);
	}

	@Override
	@Transactional
	public void unlinkFromItem(UUID groupId, UUID itemId) {
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Menu Item not found"));

		// Remove the group by comparing IDs to ensure proper deletion from the join table
		item.getModifierGroups().removeIf(g -> g.getId().equals(groupId));

		menuItemRepository.save(item);
	}
}
