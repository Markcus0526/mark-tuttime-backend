package com.tuttime.menu.service;

import com.tuttime.menu.dto.MenuItemDto;

import java.util.List;
import java.util.UUID;

public interface MenuItemService {
	MenuItemDto.MenuItemDetailResponse createMenuItem(MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles);

	MenuItemDto.MenuItemDetailResponse updateMenuItem(UUID id, MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles);

	MenuItemDto.MenuItemDetailResponse getMenuItemById(UUID id);

	void deleteMenuItem(UUID id, UUID currentUserId, String userRoles);

	List<MenuItemDto.MenuItemResponseSummary> getItemsByCategory(UUID categoryId, String search, String sortBy, String direction);

	void linkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles);

	void unlinkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles);

	void updateAvailabilityFromPos(List<UUID> itemIds, boolean isAvailable, UUID branchId);

	void updatePriceFromPos(UUID itemId, java.math.BigDecimal newPrice, UUID branchId);
}
