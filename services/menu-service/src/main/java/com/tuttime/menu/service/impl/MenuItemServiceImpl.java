package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ConflictException;
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
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
	private final MenuItemMapper menuItemMapper;
	private final MenuTagMapper menuTagMapper;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final ModifierService modifierService;

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "@menuItemServiceImpl.getBranchIdFromCategory(#request.categoryId())")
	public MenuItemDto.MenuItemDetailResponse createMenuItem(MenuItemDto.MenuItemRequest request,
	                                                         UUID currentUserId, String userRoles) {
		MenuCategory category = menuCategoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		if (menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndDeletedFalse(category.getId(), request.name())) {
			throw new ConflictException("Menu item with name '" + request.name() + "' already exists in this category.");
		}

		MenuItem item = MenuItem.builder()
				.category(category)
				.name(request.name())
				.description(request.description())
				.price(request.price())
				.available(request.available() != null ? request.available() : true)
				.priority(request.priority() != null ? request.priority() : 0)
				.build();

		return menuItemMapper.toDetailResponse(menuItemRepository.save(item));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#result.categoryId != null ? @menuItemServiceImpl.getBranchIdFromCategory(#result.categoryId) : null")
	public MenuItemDto.MenuItemDetailResponse updateMenuItem(UUID id, MenuItemDto.MenuItemRequest request,
	                                                         UUID currentUserId, String userRoles) {
		MenuItem existing = menuItemRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));

		MenuCategory targetCategory = menuCategoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(targetCategory.getBranchId(), currentUserId, userRoles);

		UUID targetCategoryId = targetCategory.getId();
		boolean nameChanged = !request.name().equals(existing.getName());
		boolean categoryChanged = !existing.getCategory().getId().equals(targetCategoryId);

		if (nameChanged || categoryChanged) {
			if (menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(targetCategoryId, request.name(), id)) {
				throw new ConflictException("Menu item with name '" + request.name() + "' already exists in the target category.");
			}
		}

		existing.setName(request.name());
		existing.setDescription(request.description());
		existing.setPrice(request.price());
		existing.setAvailable(request.available() != null ? request.available() : existing.getAvailable());
		existing.setPriority(request.priority() != null ? request.priority() : existing.getPriority());

		if (categoryChanged) {
			existing.setCategory(targetCategory);
		}

		return menuItemMapper.toDetailResponse(menuItemRepository.save(existing));
	}

	@Override
	@Transactional(readOnly = true)
	public MenuItemDto.MenuItemDetailResponse getMenuItemById(UUID itemId) {
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));
		return menuItemMapper.toDetailResponse(item);
	}

	@Override
	@Transactional
	public void deleteMenuItem(UUID id, UUID currentUserId, String userRoles) {
		MenuItem item = menuItemRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));

		UUID branchId = getBranchIdFromItem(item);
		restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

		evictCacheByBranch(branchId);

		menuItemRepository.delete(item); // soft delete via @SQLDelete
	}

	@Override
	@Transactional(readOnly = true)
	public List<MenuItemDto.TagResponse> getAllTags() {
		return menuTagRepository.findAll().stream()
				.map(menuTagMapper::toTagResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void syncMenuItemTags(UUID menuItemId, List<MenuItemDto.TagRequest> requests,
	                             UUID userId, String userRoles) {
		// 1. Fetch the menu item and verify permission on its branch
		MenuItem item = menuItemRepository.findById(menuItemId)
				.orElseThrow(() -> new InvalidRequestException("Menu item not found: " + menuItemId));
		UUID branchId = getBranchIdFromItem(item);
		restaurantGrpcClient.validateBranchPermission(branchId, userId, userRoles);

		// 2. Store old tag IDs to clean up orphan tags later
		List<UUID> oldTagIds = menuItemTagRepository.findAllByMenuItemId(menuItemId)
				.stream().map(mt -> mt.getMenuTag().getId()).toList();

		// 3. Remove all existing links for this menu item
		menuItemTagRepository.deleteByMenuItemId(menuItemId);

		// 4. Create or fetch each tag and create new links
		for (MenuItemDto.TagRequest req : requests) {
			MenuTag tag = menuTagRepository.findByNameIgnoreCase(req.name().trim())
					.orElseGet(() -> {
						try {
							return menuTagRepository.save(MenuTag.builder()
									.name(req.name().trim())
									.iconUrl(req.iconUrl())
									.build());
						} catch (DataIntegrityViolationException e) {
							// Another thread created it; fetch again
							return menuTagRepository.findByNameIgnoreCase(req.name().trim())
									.orElseThrow(() -> new InvalidRequestException("Failed to create or find tag: " + req.name()));
						}
					});

			menuItemTagRepository.save(MenuItemTag.builder()
					.id(new MenuItemTagId(menuItemId, tag.getId()))
					.menuItem(item)
					.menuTag(tag)
					.build());
		}

		// 5. Clean up any tags that are no longer used anywhere
		cleanupOrphanTags(oldTagIds);
	}

	@Override
	@Transactional(readOnly = true)
	public List<MenuItemDto.MenuItemResponse> getItemsByCategory(UUID categoryId, String search,
	                                                             String sortBy, String direction) {
		Sort sortOrder = direction.equalsIgnoreCase("desc") ?
				Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();

		return menuItemRepository.findByCategoryAndSearch(categoryId, search, sortOrder).stream()
				.map(menuItemMapper::toSummaryResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void linkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles) {
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Item not found"));
		UUID branchId = getBranchIdFromItem(item);
		restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

		modifierService.linkToItem(groupId, itemId, currentUserId, userRoles);
		evictCacheByBranch(branchId);
	}

	@Override
	@Transactional
	public void unlinkModifierGroup(UUID itemId, UUID groupId, UUID currentUserId, String userRoles) {
		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Menu Item not found"));
		UUID branchId = getBranchIdFromItem(item);
		restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

		modifierService.unlinkFromItem(groupId, itemId, currentUserId, userRoles);
		evictCacheByBranch(branchId);
	}

	@Override
	@Transactional
	public void updateAvailabilityFromPos(List<UUID> itemIds, boolean isAvailable, UUID branchId) {
		log.info("POS Sync: Setting availability to {} for {} items in branch {}",
				isAvailable, itemIds.size(), branchId);

		// Validate that all items belong to the given branch (prevent tampering)
		for (UUID itemId : itemIds) {
			MenuItem item = menuItemRepository.findById(itemId)
					.orElseThrow(() -> new InvalidRequestException("Item not found: " + itemId));
			UUID actualBranch = getBranchIdFromItem(item);
			if (!actualBranch.equals(branchId)) {
				throw new InvalidRequestException("Item " + itemId + " does not belong to branch " + branchId);
			}
		}

		if (isAvailable) {
			menuItemRepository.activateItemsFromPos(itemIds);
		} else {
			menuItemRepository.deactivateItemsFromPos(itemIds);
		}

		evictCacheByBranch(branchId);
	}

	@Override
	@Transactional
	public void updatePriceFromPos(UUID itemId, BigDecimal newPrice, UUID branchId) {
		log.info("POS Sync: Updating price for item {} to {}", itemId, newPrice);

		MenuItem item = menuItemRepository.findById(itemId)
				.orElseThrow(() -> new InvalidRequestException("Item not found for price sync: " + itemId));

		UUID actualBranch = getBranchIdFromItem(item);
		if (!actualBranch.equals(branchId)) {
			throw new InvalidRequestException("Item does not belong to branch " + branchId);
		}

		item.setPrice(newPrice);
		menuItemRepository.save(item);
		evictCacheByBranch(branchId);
	}

	// ---------- cache eviction helper ----------
	@Transactional(readOnly = true)
	@CacheEvict(value = "branch_menus", key = "#branchId")
	public void evictCacheByBranch(UUID branchId) {
		// method body empty; annotation does the work
	}

	// ---------- helpers for SpEL and validation ----------
	@Transactional(readOnly = true)
	public UUID getBranchIdFromCategory(UUID categoryId) {
		return menuCategoryRepository.findById(categoryId)
				.map(MenuCategory::getBranchId)
				.orElse(null);
	}

	private UUID getBranchIdFromItem(MenuItem item) {
		// Fetch category if not already loaded (safe fallback)
		return menuCategoryRepository.findById(item.getCategory().getId())
				.map(MenuCategory::getBranchId)
				.orElseThrow(() -> new InvalidRequestException("Category not found for item"));
	}

	private void cleanupOrphanTags(List<UUID> tagIdsToCheck) {
		for (UUID tagId : tagIdsToCheck) {
			if (!menuItemTagRepository.existsByMenuTagId(tagId)) {
				menuTagRepository.deleteById(tagId);
			}
		}
	}
}