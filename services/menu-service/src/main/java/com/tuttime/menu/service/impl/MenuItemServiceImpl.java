package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.*;
import com.tuttime.menu.entity.enums.MediaType;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.MenuItemMapper;
import com.tuttime.menu.mapper.MenuTagMapper;
import com.tuttime.menu.provider.StorageProvider;
import com.tuttime.menu.repository.*;
import com.tuttime.menu.service.MenuItemService;
import com.tuttime.menu.service.ModifierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
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
	private final MediaRepository mediaRepository;
	private final StorageProvider storageProvider;

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "@menuItemServiceImpl.getBranchIdFromCategory(#request.categoryId())")
	public MenuItemDto.MenuItemDetailResponse createMenuItem(MenuItemDto.MenuItemRequest request,
	                                                         List<MultipartFile> files,
	                                                         UUID currentUserId, String userRoles) {
		// 1. Existing validation (category, permissions, name uniqueness)...
		MenuCategory category = menuCategoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new InvalidRequestException("Category not found"));
		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		if (menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndDeletedFalse(category.getId(), request.name())) {
			throw new ConflictException("Menu item with name '" + request.name() + "' already exists.");
		}

		// 2. Build and save the item (no images yet)
		MenuItem item = MenuItem.builder()
				.category(category)
				.name(request.name())
				.description(request.description())
				.price(request.price())
				.available(request.available() != null ? request.available() : true)
				.priority(request.priority() != null ? request.priority() : 0)
				.build();
		MenuItem savedItem = menuItemRepository.save(item);

		// 3. Save images if provided
		if (files != null && !files.isEmpty()) {
			saveImages(savedItem.getId(), files, savedItem.getName());
		}

		// 4. Return response – the mapper will automatically include the medias
		return menuItemMapper.toDetailResponse(savedItem);
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#result.categoryId != null ? @menuItemServiceImpl.getBranchIdFromCategory(#result.categoryId) : null")
	public MenuItemDto.MenuItemDetailResponse updateMenuItem(UUID id, MenuItemDto.MenuItemRequest request,
	                                                         List<MultipartFile> files,
	                                                         UUID currentUserId, String userRoles) {

		// 1. Fetch existing item
		MenuItem existing = menuItemRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Item not found: " + id));

		// 2. Determine target category (if provided)
		MenuCategory targetCategory = null;
		UUID targetCategoryId = null;
		if (request.categoryId() != null) {
			targetCategory = menuCategoryRepository.findById(request.categoryId())
					.orElseThrow(() -> new InvalidRequestException("Category not found: " + request.categoryId()));
			targetCategoryId = targetCategory.getId();
			restaurantGrpcClient.validateBranchPermission(targetCategory.getBranchId(), currentUserId, userRoles);
		} else {
			// Validate permission on the existing branch (to ensure user still has rights)
			UUID branchId = getBranchIdFromItem(existing);
			restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);
		}

		// 3. Check name uniqueness if name is provided and changed
		String newName = request.name();
		if (newName != null && !newName.equals(existing.getName())) {
			UUID checkCategoryId = (targetCategoryId != null) ? targetCategoryId : existing.getCategory().getId();
			if (menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(checkCategoryId, newName, id)) {
				throw new ConflictException("Menu item with name '" + newName + "' already exists in the target category.");
			}
			existing.setName(newName);
		}

		// 4. Update other fields only if provided
		if (request.description() != null) {
			existing.setDescription(request.description());
		}
		if (request.price() != null) {
			existing.setPrice(request.price());
		}
		if (request.available() != null) {
			existing.setAvailable(request.available());
		}
		if (request.priority() != null) {
			existing.setPriority(request.priority());
		}
		if (targetCategory != null && !existing.getCategory().getId().equals(targetCategory.getId())) {
			existing.setCategory(targetCategory);
		}

		// 5. Save updated item
		MenuItem updatedItem = menuItemRepository.save(existing);

		// 6. Handle image updates
		//    - files == null → keep existing images
		//    - files != null && files.isEmpty() → delete all existing images
		//    - files != null && !files.isEmpty() → replace all existing images with new ones
		if (files != null) {
			// Delete existing images (both from storage and DB)
			List<Media> existingImages = mediaRepository.findByEntityIdAndMediaType(updatedItem.getId(), MediaType.IMAGE);
			for (Media media : existingImages) {
				try {
					storageProvider.delete(media.getUrl());
				} catch (Exception e) {
					log.warn("Failed to delete old image: {}", media.getUrl(), e);
				}
			}
			mediaRepository.deleteByEntityIdAndMediaType(updatedItem.getId(), MediaType.IMAGE);

			// If new files were provided, save them
			if (!files.isEmpty()) {
				saveImages(updatedItem.getId(), files, updatedItem.getName());
			}
		}

		// 7. Return response (the mapper will include medias)
		return menuItemMapper.toDetailResponse(updatedItem);
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

		List<Media> images = mediaRepository.findByEntityIdAndMediaType(item.getId(), MediaType.IMAGE);
		for (Media media : images) {
			storageProvider.delete(media.getUrl());
		}
		mediaRepository.deleteByEntityIdAndMediaType(item.getId(), MediaType.IMAGE);

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

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#result[0].branchId != null ? #result[0].branchId : null")
	public List<MenuItemDto.MenuItemResponse> updatePrioritiesForCategory(
			UUID categoryId,
			List<UUID> itemIdsInOrder,
			UUID currentUserId,
			String userRoles) {

		// 1. Fetch category & validate permission on its branch
		MenuCategory category = menuCategoryRepository.findById(categoryId)
				.orElseThrow(() -> new InvalidRequestException("Category not found: " + categoryId));
		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		// 2. Fetch all items in this category (excluding deleted)
		List<MenuItem> items = menuItemRepository.findByCategoryIdAndDeletedFalse(categoryId);
		if (items.isEmpty()) {
			return Collections.emptyList();
		}

		// 3. Build priority map (index = new priority)
		Map<UUID, Integer> priorityMap = new HashMap<>();
		for (int i = 0; i < itemIdsInOrder.size(); i++) {
			priorityMap.put(itemIdsInOrder.get(i), i);
		}

		// 4. Validate every given item belongs to this category
		Set<UUID> existingIds = items.stream().map(MenuItem::getId).collect(Collectors.toSet());
		for (UUID requestedId : itemIdsInOrder) {
			if (!existingIds.contains(requestedId)) {
				throw new InvalidRequestException("Item " + requestedId + " does not belong to category " + categoryId);
			}
		}

		// 5. Update priorities for items that are in the map (others keep original priority)
		for (MenuItem item : items) {
			Integer newPriority = priorityMap.get(item.getId());
			if (newPriority != null) {
				item.setPriority(newPriority);
			}
		}

		// 6. Bulk save
		List<MenuItem> saved = menuItemRepository.saveAll(items);

		// 7. Convert to summary response (same as getItemsByCategory uses)
		return saved.stream()
				.map(menuItemMapper::toSummaryResponse)
				.collect(Collectors.toList());
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

	// Helper method to persist images
	private void saveImages(UUID entityId, List<MultipartFile> files, String altText) {
		for (int i = 0; i < files.size(); i++) {
			MultipartFile file = files.get(i);
			String storedUrl = storageProvider.store(file);  // returns a publicly accessible URL or key
			Media media = Media.builder()
					.entityId(entityId)
					.url(storedUrl)
					.altText(altText)
					.primaryEntity(i == 0)   // first image becomes the primary one
					.mediaType(MediaType.IMAGE)
					.build();
			mediaRepository.save(media);
		}
	}
}