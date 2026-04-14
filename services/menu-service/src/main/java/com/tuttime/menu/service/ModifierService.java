package com.tuttime.menu.service;

import com.tuttime.menu.dto.ModifierDto;

import java.util.List;
import java.util.UUID;

public interface ModifierService {
	// Group Operations
	ModifierDto.ModifierGroupResponse createGroup(ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles);

	ModifierDto.ModifierGroupResponse getGroupById(UUID groupId);

	List<ModifierDto.ModifierGroupResponse> getAllGroups();

	ModifierDto.ModifierGroupResponse updateGroup(UUID groupId, ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles);

	void deleteGroup(UUID groupId, UUID currentUserId, String userRoles);

	// Modifier Operations
	ModifierDto.ModifierResponse addModifier(UUID groupId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles);

	ModifierDto.ModifierResponse updateModifier(UUID modifierId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles);

	void deleteModifier(UUID modifierId, UUID currentUserId, String userRoles);

}
