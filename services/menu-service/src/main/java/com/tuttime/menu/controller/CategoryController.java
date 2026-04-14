package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.menu.dto.CategoryDto;
import com.tuttime.menu.dto.FullMenuDto;
import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.service.MenuCategoryService;
import com.tuttime.menu.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menus")
@RequiredArgsConstructor
@Tag(name = "1.Menu Categories", description = "Endpoints for managing top-level menu groupings and bulk menu fetches")
public class CategoryController {

	private final MenuCategoryService categoryService;

	@PostMapping
	@Operation(summary = "Create a new category")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(schema = @Schema(implementation = CategoryDto.CategoryRequest.class))
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Category created",
					content = @Content(schema = @Schema(implementation = CategoryDto.CategoryResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid input data")
	})
	public ResponseEntity<ApiRes<CategoryDto.CategoryResponse>> createCategory(@Valid @RequestBody CategoryDto.CategoryRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		CategoryDto.CategoryResponse data = categoryService.createCategory(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@GetMapping("/{categoryId}")
	@Operation(summary = "Get category by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(schema = @Schema(implementation = CategoryDto.CategoryResponse.class))),
			@ApiResponse(responseCode = "404", description = "Category not found")
	})
	public ResponseEntity<ApiRes<CategoryDto.CategoryResponse>> getCategoryById(@PathVariable UUID categoryId) {

		CategoryDto.CategoryResponse data = categoryService.getCategoryById(categoryId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@PutMapping("/{categoryId}")
	@Operation(summary = "Update an existing category")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Update successful",
					content = @Content(schema = @Schema(implementation = CategoryDto.CategoryResponse.class))),
			@ApiResponse(responseCode = "404", description = "Category not found")
	})
	public ResponseEntity<ApiRes<CategoryDto.CategoryResponse>> updateCategory(
			@PathVariable UUID categoryId,
			@Valid @RequestBody CategoryDto.CategoryRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		CategoryDto.CategoryResponse data = categoryService.updateCategory(categoryId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{categoryId}")
	@Operation(summary = "Delete a category")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Category deleted"),
			@ApiResponse(responseCode = "404", description = "Category not found")
	})
	public ResponseEntity<ApiRes<Void>> deleteCategory(@PathVariable UUID categoryId) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		categoryService.deleteCategory(categoryId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@GetMapping("/branches/{branchId}")
	@Operation(summary = "Get categories by branch")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryDto.CategoryResponse.class))))
	})
	public ResponseEntity<ApiRes<List<CategoryDto.CategoryResponse>>> getCategoriesByBranch(@PathVariable UUID branchId) {

		List<CategoryDto.CategoryResponse> data = categoryService.getCategoriesByBranch(branchId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@GetMapping("/branches/{branchId}/full-menu")
	@Operation(summary = "Master Endpoint: Fetch complete branch menu")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Full menu tree retrieved",
					content = @Content(schema = @Schema(implementation = FullMenuDto.FullMenuBranchMenuResponse.class))),
			@ApiResponse(responseCode = "404", description = "Branch not found")
	})
	public ResponseEntity<ApiRes<FullMenuDto.FullMenuBranchMenuResponse>> getFullMenu(
			@Parameter(description = "UUID of the branch") @PathVariable UUID branchId) {

		FullMenuDto.FullMenuBranchMenuResponse data = categoryService.getFullMenuForBranch(branchId);
		return ResponseEntity.ok(ApiRes.success(data));
	}
}
