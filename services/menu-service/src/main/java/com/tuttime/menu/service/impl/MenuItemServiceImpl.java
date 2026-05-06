package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.*;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.MenuItemMapper;
import com.tuttime.menu.mapper.MenuTagMapper;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.MenuItemTagRepository;
import com.tuttime.menu.repository.MenuTagRepository;
import com.tuttime.menu.service.MenuItemService;
import com.tuttime.menu.service.ModifierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuItemServiceImpl implements MenuItemService {

	private final MenuItemRepository menuItemRepository;
	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuTagRepository menuTagRepository;
	private final MenuItemTagRepository menuItemTagRepository;
	private final MenuItemMapper menuItemMapper; // Use this instead of manual mapping
	private final MenuTagMapper menuTagMapper;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final ModifierService modifierService; // Needed for link/unlink

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "@menuItemServiceImpl.getBranchIdFromCategory(#request.categoryId())")
	public MenuItemDto.MenuItemDetailResponse createMenuItem(MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles) {
		MenuCategory category = menuCategoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		MenuItem item = MenuItem.builder()
				.category(category)
				.name(request.name())
				.description(request.description())
				.price(request.price())
				.available(request.available() != null ? request.available() : true)
				.priority(request.priority() != null ? request.priority() : 0)
				.build();

		// Use the mapper to get automatic pricing, media, and modifier inheritance
		return menuItemMapper.toDetailResponse(menuItemRepository.save(item));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#result.categoryId != null ? @menuItemServiceImpl.getBranchIdFromCategory(#result.categoryId) : null")
	public MenuItemDto.MenuItemDetailResponse updateMenuItem(UUID id, MenuItemDto.MenuItemRequest request, UUID currentUserId, String userRoles) {
		MenuItem existing = menuItemRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));

		MenuCategory category = menuCategoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		existing.setName(request.name());
		existing.setDescription(request.description());
		existing.setPrice(request.price());
		existing.setAvailable(request.available());

		return menuItemMapper.toDetailResponse(menuItemRepository.save(existing));
	}

	@Override
	@Transactional(readOnly = true)
	public MenuItemDto.MenuItemDetailResponse getMenuItemById(UUID itemId) {
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));

		// This now returns discountedPrice, originalPrice, and merged Modifiers
		return menuItemMapper.toDetailResponse(item);
	}

	@Override
	@Transactional
	public void deleteMenuItem(UUID id, UUID currentUserId, String userRoles) {
		MenuItem item = menuItemRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));

		MenuCategory category = menuCategoryRepository.findById(item.getCategory().getId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		// Manual eviction helper if SpEL is too complex for delete
		evictCacheByBranch(item.getCategory().getBranchId());

		menuItemRepository.delete(item);
	}

	@Transactional(readOnly = true)
	public List<MenuItemDto.TagResponse> getAllTags() {
		// Fetch all tags and map them to DTOs for the UI
		return menuTagRepository.findAll().stream()
				.map(menuTagMapper::toTagResponse) // Uses the iconUrl and name mapping
				.toList();
	}

	@Transactional
	public void syncMenuItemTags(UUID menuItemId, List<MenuItemDto.TagRequest> requests, UUID userId, String roles) {
		// 1. Security: Verify user has permission for THIS specific branch if not Admin
		List<UUID> oldTagIds = menuItemTagRepository.findAllByMenuItemId(menuItemId)
				.stream().map(bt -> bt.getMenuTag().getId()).toList();

		// Logic #2: Only remove the links for this specific branch
		// Efficiently clears the bridge table entries
		menuItemTagRepository.deleteByMenuItemId(menuItemId);

		for (MenuItemDto.TagRequest req : requests) {
			MenuTag tag = menuTagRepository.findByNameIgnoreCase(req.name().trim())
					.orElseGet(() -> menuTagRepository.save(MenuTag.builder()
							.name(req.name().trim())
							.iconUrl(req.iconUrl())
							.build()));

			menuItemTagRepository.save(MenuItemTag.builder()
					.id(new MenuItemTagId(menuItemId, tag.getId()))
					.menuItem(menuItemRepository.getReferenceById(menuItemId))
					.menuTag(tag)
					.build());
		}

		cleanupOrphanTags(oldTagIds);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MenuItemDto.MenuItemResponse> getItemsByCategory(UUID categoryId, String search, String sortBy, String direction) {
		Sort sortOrder = direction.equalsIgnoreCase("desc") ?
				Sort.by(sortBy).descending() :
				Sort.by(sortBy).ascending();

		// Use the mapper to handle discounted 'currentPrice' and 'imageUrl'
		return menuItemRepository.findByCategoryAndSearch(categoryId, search, sortOrder).stream()
				.map(menuItemMapper::toSummaryResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void linkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles) {
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));

		MenuCategory category = menuCategoryRepository.findById(item.getCategory().getId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		// Assuming ModifierGroup is fetched and added to item.modifierGroups
		// Or call a specialized method in modifierService
		modifierService.linkToItem(groupId, itemId);
		evictCacheByBranch(item.getCategory().getBranchId());
	}

	@Override
	@Transactional
	public void unlinkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles) {
		// 1. Fetch the item
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Menu Item not found"));

		MenuCategory category = menuCategoryRepository.findById(item.getCategory().getId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		// 2. Remove the association from the set
		// This triggers a DELETE on the 'menu_item_modifier_groups' table
		item.getModifierGroups().removeIf(group -> group.getId().equals(groupId));

		// 3. Save to persist changes and evict cache
		menuItemRepository.save(item);
		evictCacheByBranch(item.getCategory().getBranchId());
	}

	@Transactional(readOnly = true)
	@CacheEvict(value = "branch_menus", key = "#branchId")
	public void evictCacheByBranch(UUID branchId) {
		// This method body is empty because the @CacheEvict
		// annotation does all the work before/after the method runs.
	}

	/**
	 * Helper for Cache Eviction SpEL
	 */
	@Transactional(readOnly = true)
	public UUID getBranchIdFromCategory(UUID categoryId) {
		return menuCategoryRepository.findById(categoryId)
				.map(MenuCategory::getBranchId)
				.orElse(null);
	}

	@Override
	@Transactional
	public void updateAvailabilityFromPos(List<UUID> itemIds, boolean isAvailable, UUID branchId) {
		log.info("POS Sync: Setting availability to {} for {} items in branch {}",
				isAvailable, itemIds.size(), branchId);

		// Bulk update using the repository method we created earlier
		if (isAvailable) {
			menuItemRepository.activateItemsFromPos(itemIds);
		} else {
			menuItemRepository.deactivateItemsFromPos(itemIds);
		}

		// IMPORTANT: Evict cache so customers see the change immediately
		evictCacheByBranch(branchId);
	}

	@Override
	@Transactional
	public void updatePriceFromPos(UUID itemId, java.math.BigDecimal newPrice, UUID branchId) {
		log.info("POS Sync: Updating price for item {} to {}", itemId, newPrice);

		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Item not found for price sync: " + itemId));

		item.setPrice(newPrice);
		menuItemRepository.save(item);

		// Evict cache to reflect the new price in the app
		evictCacheByBranch(branchId);
	}

	private void cleanupOrphanTags(List<UUID> tagIdsToCheck) {
		for (UUID tagId : tagIdsToCheck) {
			// If no branch is using this tag anymore, delete it from master table
			if (!menuItemTagRepository.existsByMenuTagId(tagId)) {
				menuTagRepository.deleteById(tagId);
			}
		}
	}
}
