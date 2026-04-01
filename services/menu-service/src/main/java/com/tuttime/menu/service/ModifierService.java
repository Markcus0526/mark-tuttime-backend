package com.tuttime.menu.service;

import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.dto.ModifierGroupDto;
import java.util.UUID;

public interface ModifierService {
	// Group Logic
	ModifierGroupDto.ModifierGroupResponse createGroup(ModifierGroupDto.ModifierGroupRequest request);
	ModifierGroupDto.ModifierGroupResponse getGroupById(UUID id);
	void updateGroup(UUID id, ModifierGroupDto.ModifierGroupRequest request);
	void deleteGroup(UUID id);

	// Modifier Logic
	ModifierDto.ModifierResponse addModifierToGroup(UUID groupId, ModifierDto.ModifierRequest request);
	void deleteModifier(UUID modifierId);

	// Linking Logic (Item <-> Group)
	void assignGroupToItem(UUID itemId, UUID groupId);
}
