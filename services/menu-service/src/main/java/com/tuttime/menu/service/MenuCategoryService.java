package com.tuttime.menu.service;

import com.tuttime.menu.dto.CategoryDto;
import com.tuttime.menu.dto.FullMenuDto;

import java.util.List;
import java.util.UUID;

public interface MenuCategoryService {
	CategoryDto.CategoryResponse createCategory(CategoryDto.CategoryRequest request, UUID currentUserId, String userRoles);

	CategoryDto.CategoryResponse updateCategory(UUID id, CategoryDto.CategoryRequest request, UUID currentUserId, String userRoles);

	CategoryDto.CategoryResponse getCategoryById(UUID id);

	List<CategoryDto.CategoryResponse> getCategoriesByBranch(UUID branchId);

	FullMenuDto.FullMenuBranchMenuResponse getFullMenuForBranch(UUID branchId); // Master Endpoint

	void deleteCategory(UUID id, UUID currentUserId, String userRoles);

	void validateMenuCategory(UUID id);
}