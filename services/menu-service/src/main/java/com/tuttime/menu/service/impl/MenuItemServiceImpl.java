package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuItemServiceImpl implements MenuItemService {

	private final MenuItemRepository menuItemRepository;
	private final MenuCategoryRepository categoryRepository;
	private final RestaurantGrpcClient restaurantGrpcClient;

	@Override
	@Transactional
	// Evict the branch menu cache. We find the branchId through the category.
	@CacheEvict(value = "branch_menus", key = "@menuItemServiceImpl.getBranchIdFromCategory(#request.categoryId())")
	public MenuItemDto.MenuItemDetailedResponse createMenuItem(MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles) {
		MenuCategory category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		MenuItem item = MenuItem.builder()
				.category(category)
				.name(request.name())
				.description(request.description())
				.price(request.price())
				.available(request.available() != null ? request.available() : true)
				.build();

		return mapToDetailedResponse(menuItemRepository.save(item));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#result.categoryId != null ? @menuItemServiceImpl.getBranchIdFromCategory(#result.categoryId) : null")
	public MenuItemDto.MenuItemDetailedResponse updateMenuItem(UUID id, MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles) {
		MenuItem existing = menuItemRepository.findById(id)
				.orElseThrow(() -> new MethodArgumentNotValidException("Item not found"));

		MenuCategory category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		existing.setName(request.name());
		existing.setDescription(request.description());
		existing.setPrice(request.price());
		existing.setAvailable(request.available());

		return mapToDetailedResponse(menuItemRepository.save(existing));
	}

	@Override
	public MenuItemDto.MenuItemDetailedResponse getMenuItemById(UUID id) {
		return menuItemRepository.findById(id)
				.map(this::mapToDetailedResponse)
				.orElseThrow(() -> new MethodArgumentNotValidException("Item not found"));
	}

	@Override
	@Transactional
	public void deleteMenuItem(UUID id, UUID currentUserId, String userRoles) {
		MenuItem item = menuItemRepository.findById(id)
				.orElseThrow(() -> new MethodArgumentNotValidException("Item not found"));

		MenuCategory category = categoryRepository.findById(item.getCategory().getId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		// Manual eviction helper if SpEL is too complex for delete
		evictCacheByBranch(item.getCategory().getBranchId());

		menuItemRepository.delete(item);
	}

	@Override
	public List<MenuItemDto.MenuItemResponseSummary> getItemsByCategory(UUID categoryId, String sortBy, String direction) {
		// 1. Define sort logic
		Sort sort = direction.equalsIgnoreCase("desc") ?
				Sort.by(sortBy).descending() :
				Sort.by(sortBy).ascending();

		// 2. Fetch sorted list
		return menuItemRepository.findByCategoryId(categoryId, sort).stream()
				.map(item -> new MenuItemDto.MenuItemResponseSummary(
						item.getId(), item.getName(), item.getPrice(), item.getAvailable(), item.getPriority()
				))
				.collect(Collectors.toList());
	}

	/**
	 * Helper for Cache Eviction SpEL
	 */
	public UUID getBranchIdFromCategory(UUID categoryId) {
		return categoryRepository.findById(categoryId)
				.map(MenuCategory::getBranchId)
				.orElse(null);
	}

	@CacheEvict(value = "branch_menus", key = "#branchId")
	public void evictCacheByBranch(UUID branchId) {}

	private MenuItemDto.MenuItemDetailedResponse mapToDetailedResponse(MenuItem item) {
		return new MenuItemDto.MenuItemDetailedResponse(
				item.getId(),
				item.getCategory().getId(),
				item.getName(),
				item.getDescription(),
				item.getPrice(),
				item.getAvailable(),
				List.of() // Modifiers mapped here if needed
		);
	}
}
