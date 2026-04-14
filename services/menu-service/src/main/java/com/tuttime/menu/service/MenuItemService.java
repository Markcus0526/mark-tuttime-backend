package com.tuttime.menu.service;

import com.tuttime.menu.dto.MenuItemDto;

import java.util.List;
import java.util.UUID;

public interface MenuItemService {
	MenuItemDto.MenuItemDetailedResponse createMenuItem(MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles);

	MenuItemDto.MenuItemDetailedResponse updateMenuItem(UUID id, MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles);

	MenuItemDto.MenuItemDetailedResponse getMenuItemById(UUID id);

	List<MenuItemDto.MenuItemResponseSummary> getItemsByCategory(UUID categoryId, String sortBy, String direction);

	void deleteMenuItem(UUID id, UUID currentUserId, String userRoles);
}
