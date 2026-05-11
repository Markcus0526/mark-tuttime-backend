package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.dto.FullMenuDto;
import com.tuttime.menu.dto.MenuCategoryDto;
import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.MenuItemTag;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.MenuCategoryMapper;
import com.tuttime.menu.mapper.MenuItemMapper;
import com.tuttime.menu.repository.DiscountRepository;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemTagRepository;
import com.tuttime.menu.service.MenuCategoryService;
import com.tuttime.menu.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuCategoryServiceImpl implements MenuCategoryService {

	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuCategoryMapper menuCategoryMapper;
	private final MenuItemTagRepository menuItemTagRepository;
	private final DiscountRepository discountRepository;
	private final MenuItemMapper menuItemMapper;
	private final PricingService pricingService;
	private final RestaurantGrpcClient restaurantGrpcClient;

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#request.branchId()")
	public MenuCategoryDto.CategoryResponse createCategory(MenuCategoryDto.CategoryRequest request, UUID currentUserId, String userRoles) {
		// 1. Security Check via gRPC
		restaurantGrpcClient.validateBranchPermission(request.branchId(), currentUserId, userRoles);

		// 2. Unique name check (case-insensitive, ignoring deleted categories)
		if (menuCategoryRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalse(request.branchId(), request.name())) {
			throw new ConflictException("Category with name '" + request.name() + "' already exists in this branch.");
		}

		// 3. Build Entity
		MenuCategory category = MenuCategory.builder()
				.branchId(request.branchId())
				.name(request.name())
				.description(request.description())
				.priority(request.priority() != null ? request.priority() : 0)
				.build();

		// 3. Save and Map
		return menuCategoryMapper.toResponse(menuCategoryRepository.save(category));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#result.branchId()")
	public MenuCategoryDto.CategoryResponse updateCategory(UUID id, MenuCategoryDto.CategoryRequest request, UUID currentUserId, String userRoles) {
		MenuCategory existing = menuCategoryRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Category not found with id: " + id));

		// Ensure user has permission for the branch this category belongs to
		restaurantGrpcClient.validateBranchPermission(existing.getBranchId(), currentUserId, userRoles);

		// Unique name check (if name is changed)
		if (!request.name().equals(existing.getName())) {
			if (menuCategoryRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(existing.getBranchId(), request.name(), id)) {
				throw new ConflictException("Category with name '" + request.name() + "' already exists in this branch.");
			}
			existing.setName(request.name());
		}

		existing.setDescription(request.description());
		existing.setPriority(request.priority());

		return menuCategoryMapper.toResponse(menuCategoryRepository.save(existing));
	}

	@Override
	@Transactional(readOnly = true)
	public MenuCategoryDto.CategoryResponse getCategoryById(UUID id) {
		return menuCategoryRepository.findById(id)
				.map(menuCategoryMapper::toResponse)
				.orElseThrow(() -> new InvalidRequestException("Category not found with id: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "branch_menus", key = "#branchId")
	public List<MenuCategoryDto.CategoryResponse> getCategoriesByBranch(UUID branchId, String query, String sortBy, String direction) {

		// 1. Build the Sort object (Default to Priority)
		Sort sortOrder = (sortBy == null || sortBy.equals("priority"))
				? Sort.by(Sort.Direction.DESC, "priority")
				: (direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

		// 2. Fetch categories
		List<MenuCategory> categories = menuCategoryRepository.findActiveByBranchWithSearch(branchId, query, sortOrder);

		// 3. Map to DTOs and include item counts
		return categories.stream()
				.map(category -> {
					// Efficiently get count of non-deleted items
					long count = category.getMenuItems().stream()
							.filter(item -> !item.isDeleted())
							.count();

					return menuCategoryMapper.toResponseWithCount(category, (int) count);
				})
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "branch_menus", key = "#branchId")
	public FullMenuDto.FullMenuBranchMenuResponse getFullMenuForBranch(UUID branchId) {
		// 1. Fetch Categories & Items (Already sorted by priority in Repo)
		List<MenuCategory> categories = menuCategoryRepository.findAllByBranchIdWithItemsSorted(branchId);

		if (categories.isEmpty()) {
			return new FullMenuDto.FullMenuBranchMenuResponse(branchId, Collections.emptyList());
		}

		// 2. Collect IDs for Batch Fetching
		List<UUID> categoryIds = categories.stream().map(MenuCategory::getId).toList();
		List<UUID> itemIds = categories.stream()
				.flatMap(c -> c.getMenuItems().stream())
				.map(MenuItem::getId)
				.toList();

		// 3. Batch Fetch Tags & Discounts (Prevents N+1)
		List<MenuItemTag> allItemTags = menuItemTagRepository.findAllByMenuItemIdIn(itemIds);
		List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(itemIds, categoryIds);

		// 4. Build the response tree
		List<FullMenuDto.CategoryWithItemsResponse> categoryResponses = categories.stream()
				.map(cat -> new FullMenuDto.CategoryWithItemsResponse(
						cat.getId(),
						cat.getName(),
						cat.getDescription(),
						cat.getPriority(),
						cat.getMenuItems().stream()
								.filter(item -> !item.isDeleted() && item.getAvailable())
								.sorted(Comparator.comparing(MenuItem::getPriority).reversed())
								.map(item -> {
									// Filter tags for this item
									List<MenuItemDto.TagResponse> itemTags = allItemTags.stream()
											.filter(it -> it.getMenuItem().getId().equals(item.getId()))
											.map(it -> new MenuItemDto.TagResponse(
													it.getMenuTag().getId(),
													it.getMenuTag().getName(),
													it.getMenuTag().getIconUrl()))
											.toList();

									// Calculate Discounted Price via PricingService (using preloaded list)
									DiscountDto.PricingResult result = pricingService.calculateFinalPrice(item, activeDiscounts);

									// Map to DTO and set the dynamic discountedPrice
									return menuItemMapper.toFullResponse(item, itemTags).toBuilder()
											.discountedPrice(result.finalPrice())
											.build();
								}).toList()
				)).toList();

		return new FullMenuDto.FullMenuBranchMenuResponse(branchId, categoryResponses);
	}

	@Override
	@Transactional
	public void deleteCategory(UUID id, UUID currentUserId, String userRoles) {
		MenuCategory category = menuCategoryRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("Category not found"));

		restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

		// Manually evict cache for this branch before soft-deleting
		evictBranchCache(category.getBranchId());

		menuCategoryRepository.delete(category);
	}

	/**
	 * Helper used for manual eviction in delete or complex update scenarios
	 */
	@CacheEvict(value = "branch_menus", key = "#branchId")
	public void evictBranchCache(UUID branchId) {
		// Intentionally blank - annotation handles logic
	}
}
