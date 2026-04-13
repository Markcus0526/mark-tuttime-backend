package com.tuttime.menu.service.impl;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.menu.dto.CategoryDto;
import com.tuttime.menu.dto.FullMenuDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.CategoryMapper;
import com.tuttime.menu.mapper.FullMenuMapper;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.service.MenuCategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuCategoryServiceImpl implements MenuCategoryService {

	private final MenuCategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;
	private final FullMenuMapper fullMenuMapper;
	private final RestaurantGrpcClient restaurantGrpcClient;

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#request.branchId()")
	public CategoryDto.CategoryResponse createCategory(CategoryDto.CategoryRequest request) {
		MenuCategory category = categoryMapper.toEntity(request);
		return categoryMapper.toResponse(categoryRepository.save(category));
	}

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#result.branchId()") // Evict based on the returned DTO's branchId
	public CategoryDto.CategoryResponse updateCategory(UUID id, CategoryDto.CategoryRequest request) {
		MenuCategory existing = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

		existing.setName(request.name());
		existing.setDescription(request.description());
		existing.setPriority(request.priority());

		return categoryMapper.toResponse(categoryRepository.save(existing));
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryDto.CategoryResponse getCategoryById(UUID id) {
		return categoryRepository.findById(id)
				.map(categoryMapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryDto.CategoryResponse> getCategoriesByBranch(UUID branchId) {
		return categoryRepository.findAllByBranchId(branchId)
				.stream()
				.map(categoryMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "branch_menus", key = "#branchId") // Re-added caching for the read operation
	public FullMenuDto.FullMenuBranchMenuResponse getFullMenuForBranch(UUID branchId) {
		// This now returns Categories sorted by priority 0, 1, 2...
		List<MenuCategory> categories = categoryRepository.findAllByBranchIdOrderByPriorityAsc(branchId);

		// In your Mapper logic, ensure items are also sorted by priority
		return fullMenuMapper.toBranchMenuResponse(branchId, categories);
	}

	@Override
	@Transactional
	public void deleteCategory(UUID id) {
		MenuCategory category = categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found"));

		// Evict cache for this branch before deleting
		evictBranchCache(category.getBranchId());

		categoryRepository.delete(category);
	}

	@Override
	public void validateBranchManager(UUID branchId) {
		if (UserContext.getUserId() == null)
			throw new AuthenticationException("You do not manage this specific restaurant or branch");

		UUID currentUserId = UUID.fromString(UserContext.getUserId());

		if (UserContext.isRoleAdmin()) return;

		restaurantGrpcClient.checkBranchExists(currentUserId, branchId);
	}

	@Override
	public void validateMenuCategory(UUID id) {
		if (UserContext.getUserId() == null)
			throw new AuthenticationException("You do not manage this specific restaurant or branch");

		if (UserContext.isRoleAdmin()) return;

		boolean exists = categoryRepository.existsById(id);

		if (!exists) {
			throw new AuthenticationException("You do not manage this specific restaurant");
		}
	}

	@CacheEvict(value = "branch_menus", key = "#branchId")
	public void evictBranchCache(UUID branchId) {
		// Helper method to trigger cache eviction by branchId
	}
}
