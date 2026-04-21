package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.FullMenuDto;
import com.tuttime.menu.dto.MenuCategoryDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.MenuCategoryMapper;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.service.MenuCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuCategoryServiceImpl implements MenuCategoryService {

	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuCategoryMapper menuCategoryMapper;
	private final RestaurantGrpcClient restaurantGrpcClient;

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#request.branchId()")
	public MenuCategoryDto.CategoryResponse createCategory(MenuCategoryDto.CategoryRequest request, UUID currentUserId, String userRoles) {
		// 1. Security Check via gRPC
		restaurantGrpcClient.validateBranchPermission(request.branchId(), currentUserId, userRoles);

		// 2. Build Entity
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

		existing.setName(request.name());
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

		// 1. Build the Sort object
		Sort sortOrder = direction.equalsIgnoreCase("desc") ?
				Sort.by(sortBy).descending() :
				Sort.by(sortBy).ascending();

		// 2. Fetch categories with dynamic search and sort
		List<MenuCategory> categories = menuCategoryRepository.findActiveByBranchWithSearch(branchId, query, sortOrder);

		// 3. Map to DTOs
		return categories.stream()
				.map(menuCategoryMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "branch_menus", key = "#branchId")
	public FullMenuDto.FullMenuBranchMenuResponse getFullMenuForBranch(UUID branchId) {
		// 1. Fetch Categories (Optimized with EntityGraph in Repository to avoid N+1)
		List<MenuCategory> categories = menuCategoryRepository.findByBranchIdAndDeletedFalseOrderByPriorityAsc(branchId);

		// 2. Map to the nested tree structure (Category -> Items -> Modifiers)
		List<FullMenuDto.CategoryWithItemsResponse> categoryData = categories.stream()
				.map(menuCategoryMapper::toCategoryWithItemsResponse)
				.toList();

		return new FullMenuDto.FullMenuBranchMenuResponse(branchId, categoryData);
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
