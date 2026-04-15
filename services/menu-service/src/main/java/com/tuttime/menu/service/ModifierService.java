package com.tuttime.menu.service;

import com.tuttime.menu.dto.ModifierDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ModifierService {
	// Group Operations
	Page<ModifierDto.ModifierGroupResponse> findGroups(String query, UUID restaurantId, Pageable pageable);

	ModifierDto.ModifierGroupResponse createGroup(ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles);

	ModifierDto.ModifierGroupResponse getGroupById(UUID groupId);

	List<ModifierDto.ModifierGroupResponse> getAllGroups();

	ModifierDto.ModifierGroupResponse updateGroup(UUID groupId, ModifierDto.ModifierGroupRequest request, UUID currentUserId, String userRoles);

	void deleteGroup(UUID groupId, UUID currentUserId, String userRoles);

	// Modifier Operations
	List<ModifierDto.ModifierResponse> getModifiersByGroupId(UUID groupId);

	ModifierDto.ModifierResponse addModifier(UUID groupId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles);

	ModifierDto.ModifierResponse updateModifier(UUID modifierId, ModifierDto.ModifierRequest request, UUID currentUserId, String userRoles);

	void deleteModifier(UUID modifierId, UUID currentUserId, String userRoles);

	void linkToItem(UUID groupId, UUID itemId);

	void unlinkFromItem(UUID groupId, UUID itemId);

}
