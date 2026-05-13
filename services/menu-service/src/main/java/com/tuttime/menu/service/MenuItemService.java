package com.tuttime.menu.service;

import com.tuttime.menu.dto.MenuItemDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MenuItemService {
	MenuItemDto.MenuItemDetailResponse createMenuItem(MenuItemDto.MenuItemRequest request, List<MultipartFile> files, UUID currentUserId, String userRoles);

	MenuItemDto.MenuItemDetailResponse updateMenuItem(UUID id, MenuItemDto.MenuItemRequest request, List<MultipartFile> files, UUID currentUserId, String userRoles);

	MenuItemDto.MenuItemDetailResponse getMenuItemById(UUID id);

	void deleteMenuItem(UUID id, UUID currentUserId, String userRoles);

	List<MenuItemDto.MenuItemResponse> getItemsByCategory(UUID categoryId, String search, String sortBy, String direction);

	List<MenuItemDto.TagResponse> getAllTags();

	void syncMenuItemTags(UUID branchId, List<MenuItemDto.TagRequest> requests, UUID userId, String roles);

	void linkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles);

	void unlinkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles);

	void updateAvailabilityFromPos(List<UUID> itemIds, boolean isAvailable, UUID branchId);

	void updatePriceFromPos(UUID itemId, java.math.BigDecimal newPrice, UUID branchId);

	List<MenuItemDto.MenuItemResponse> updatePrioritiesForCategory(
			UUID categoryId,
			List<UUID> itemIdsInOrder,
			UUID currentUserId,
			String userRoles);
}
