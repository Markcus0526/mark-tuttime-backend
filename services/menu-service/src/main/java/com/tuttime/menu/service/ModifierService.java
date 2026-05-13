package com.tuttime.menu.service;

import com.tuttime.menu.dto.ModifierDto;

import java.util.List;
import java.util.UUID;

public interface ModifierService {

	ModifierDto.ModifierGroupResponse createGroup(ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles);

	ModifierDto.ModifierGroupResponse getGroupById(UUID groupId);

	List<ModifierDto.ModifierGroupResponse> getAllGroupsByBranch(UUID branchId, UUID currentUserId, String userRoles);

	ModifierDto.ModifierGroupResponse updateGroup(UUID groupId, ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles);

	void deleteGroup(UUID groupId, UUID currentUserId, String userRoles);

	// Modifier Operations
	List<ModifierDto.ModifierResponse> getModifiersByGroupId(UUID groupId);

	ModifierDto.ModifierResponse addModifier(UUID groupId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles);

	ModifierDto.ModifierResponse updateModifier(UUID modifierId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles);

	void deleteModifier(UUID modifierId, UUID currentUserId, String userRoles);

	void linkToItem(UUID groupId, UUID itemId, UUID currentUserId, String userRoles);

	void unlinkFromItem(UUID groupId, UUID itemId, UUID currentUserId, String userRoles);

}
