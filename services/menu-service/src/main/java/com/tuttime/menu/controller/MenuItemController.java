package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
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
@RequestMapping("/api/v1/menu-items")
@RequiredArgsConstructor
@Tag(name = "2.Menu Items", description = "Endpoints for administering specific dishes and their prices")
public class MenuItemController {

	private final MenuCategoryService categoryService;
	private final MenuItemService menuItemService;

	@PostMapping
	@Operation(summary = "Create a new dish or drink", description = "Saves a menu item tied directly to an active category.")
	@io.swagger.v3.oas.annotations.parameters.RequestBody(
			content = @Content(schema = @Schema(implementation = MenuItemDto.MenuItemRequest.class))
	)
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Item created",
					content = @Content(schema = @Schema(implementation = MenuItemDto.MenuItemDetailResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload")
	})
	public ResponseEntity<ApiRes<MenuItemDto.MenuItemDetailResponse>> createMenuItem(
			@Valid @RequestBody MenuItemDto.MenuItemRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MenuItemDto.MenuItemDetailResponse data = menuItemService.createMenuItem(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@GetMapping("/{itemId}")
	@Operation(summary = "Get a menu item by ID", description = "Fetches complete object properties including relational modifier groups.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(schema = @Schema(implementation = MenuItemDto.MenuItemDetailResponse.class))),
			@ApiResponse(responseCode = "404", description = "Item not found")
	})
	public ResponseEntity<ApiRes<MenuItemDto.MenuItemDetailResponse>> getMenuItemById(
			@PathVariable UUID itemId) {

		// This service method should now include the PricingService logic
		// to calculate 'discountedPrice' and 'originalPrice' TODO
		MenuItemDto.MenuItemDetailResponse data = menuItemService.getMenuItemById(itemId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@PutMapping("/{itemId}")
	@Operation(summary = "Update an item's parameters")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Update successful",
					content = @Content(schema = @Schema(implementation = MenuItemDto.MenuItemDetailResponse.class))),
			@ApiResponse(responseCode = "404", description = "Item not found")
	})
	public ResponseEntity<ApiRes<MenuItemDto.MenuItemDetailResponse>> updateMenuItem(
			@PathVariable UUID itemId,
			@Valid @RequestBody MenuItemDto.MenuItemRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MenuItemDto.MenuItemDetailResponse data = menuItemService.updateMenuItem(itemId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{itemId}")
	@Operation(summary = "Purge an item from the menu")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Item successfully deleted"),
			@ApiResponse(responseCode = "404", description = "Item not found")
	})
	public ResponseEntity<ApiRes<Void>> deleteMenuItem(
			@PathVariable UUID itemId) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		menuItemService.deleteMenuItem(itemId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT)
				.body(ApiRes.success(null));
	}

	@Operation(summary = "Get all master tags", description = "Retrieve a list of all available tags for menu items.")
	@GetMapping("/tags")
	public ResponseEntity<ApiRes<List<MenuItemDto.TagResponse>>> getAllTags() {
		List<MenuItemDto.TagResponse> tags = menuItemService.getAllTags();
		return ResponseEntity.ok(ApiRes.success(tags));
	}

	@Operation(summary = "Sync menu tags", description = "Add, remove, or create tags for a specific menu item. If a tag name doesn't exist globally, it will be created.")
	@PutMapping("/{itemId}/tags")
	public ResponseEntity<ApiRes<Void>> syncBranchTags(
			@PathVariable UUID itemId,
			@Valid @RequestBody List<MenuItemDto.TagRequest> tags) {

		// 1. Existing security check in your controller
		validateAdminOrOwnerOrManager();

		// 2. Extract context for service-level checks
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		// 3. Delegate to service for the "find or create" logic
		menuItemService.syncMenuItemTags(itemId, tags, currentUserId, userRoles);

		return ResponseEntity.ok(ApiRes.success(null));
	}

	@GetMapping("/categories/{categoryId}")
	@Operation(summary = "Get searched and sorted items in a category")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "List of items retrieved",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = MenuItemDto.MenuItemResponse.class))))
	})
	public ResponseEntity<ApiRes<List<MenuItemDto.MenuItemResponse>>> getItemsByCategory(
			@PathVariable UUID categoryId,
			@Parameter(description = "Search keyword for item name or description") @RequestParam(required = false) String search,
			@Parameter(description = "Field to sort by (e.g., name, price, priority)") @RequestParam(defaultValue = "name") String sortBy,
			@Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {

		// Pass the 'search' parameter to the service logic
		List<MenuItemDto.MenuItemResponse> data = menuItemService.getItemsByCategory(categoryId, search, sortBy, direction);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@PostMapping("/{itemId}/modifier-groups/{groupId}")
	@Operation(summary = "Link a modifier group to an item", description = "Associates a customization rule directly with this dish.")
	public ResponseEntity<ApiRes<Void>> linkModifierGroup(
			@PathVariable UUID itemId,
			@PathVariable UUID groupId) {

		validateAdminOrOwnerOrManager(); // Refactored security check
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		menuItemService.linkModifierGroup(itemId, groupId, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(null));
	}

	@DeleteMapping("/{itemId}/modifier-groups/{groupId}")
	@Operation(summary = "Unlink a modifier group from an item")
	public ResponseEntity<ApiRes<Void>> unlinkModifierGroup(
			@PathVariable UUID itemId,
			@PathVariable UUID groupId) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		menuItemService.unlinkModifierGroup(itemId, groupId, currentUserId, userRoles);
		return ResponseEntity.noContent().build();
	}

	/**
	 * Private helper to keep the controller DRY
	 */
	private void validateLogin() {
		if (!UserContext.isValidUUID()) {
			throw new UnauthorizedException("Unauthorized: You should login");
		}
	}

	private void validateAdmin() {
		if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}
}
