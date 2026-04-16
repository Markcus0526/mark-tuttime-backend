package com.tuttime.menu.service;

import com.tuttime.menu.dto.MenuCategoryDto;
import com.tuttime.menu.dto.FullMenuDto;

import java.util.List;
import java.util.UUID;

public interface MenuCategoryService {
	MenuCategoryDto.CategoryResponse createCategory(MenuCategoryDto.CategoryRequest request, UUID currentUserId, String userRoles);

	MenuCategoryDto.CategoryResponse updateCategory(UUID id, MenuCategoryDto.CategoryRequest request, UUID currentUserId, String userRoles);

	MenuCategoryDto.CategoryResponse getCategoryById(UUID id);

	List<MenuCategoryDto.CategoryResponse> getCategoriesByBranch(UUID branchId, String query, String sortBy, String direction);

	FullMenuDto.FullMenuBranchMenuResponse getFullMenuForBranch(UUID branchId); // Master Endpoint

	void deleteCategory(UUID id, UUID currentUserId, String userRoles);
}