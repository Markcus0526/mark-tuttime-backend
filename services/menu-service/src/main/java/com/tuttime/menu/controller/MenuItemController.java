package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
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
					content = @Content(schema = @Schema(implementation = MenuItemDto.MenuItemDetailedResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload")
	})
	public ResponseEntity<ApiRes<MenuItemDto.MenuItemDetailedResponse>> createMenuItem(
			@Valid @RequestBody MenuItemDto.MenuItemRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MenuItemDto.MenuItemDetailedResponse data = menuItemService.createMenuItem(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@GetMapping("/{itemId}")
	@Operation(summary = "Get a menu item by ID", description = "Fetches complete object properties including relational modifier groups.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(schema = @Schema(implementation = MenuItemDto.MenuItemDetailedResponse.class))),
			@ApiResponse(responseCode = "404", description = "Item not found")
	})
	public ResponseEntity<ApiRes<MenuItemDto.MenuItemDetailedResponse>> getMenuItemById(
			@PathVariable UUID itemId) {

		MenuItemDto.MenuItemDetailedResponse data = menuItemService.getMenuItemById(itemId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@PutMapping("/{itemId}")
	@Operation(summary = "Update an item's parameters")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Update successful",
					content = @Content(schema = @Schema(implementation = MenuItemDto.MenuItemDetailedResponse.class))),
			@ApiResponse(responseCode = "404", description = "Item not found")
	})
	public ResponseEntity<ApiRes<MenuItemDto.MenuItemDetailedResponse>> updateMenuItem(
			@PathVariable UUID itemId,
			@Valid @RequestBody MenuItemDto.MenuItemRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		MenuItemDto.MenuItemDetailedResponse data = menuItemService.updateMenuItem(itemId, request, currentUserId, userRoles);
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

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		menuItemService.deleteMenuItem(itemId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT)
				.body(ApiRes.success(null));
	}

	@GetMapping("/categorys/{categoryId}")
	@Operation(summary = "Get sorted items in a category")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "List of items retrieved",
					content = @Content(array = @ArraySchema(schema = @Schema(implementation = MenuItemDto.MenuItemResponseSummary.class))))
	})
	public ResponseEntity<ApiRes<List<MenuItemDto.MenuItemResponseSummary>>> getItemsByCategory(
			@PathVariable UUID categoryId,
			@Parameter(description = "Field to sort by (e.g., name, price, priority)") @RequestParam(defaultValue = "name") String sortBy,
			@Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {

		List<MenuItemDto.MenuItemResponseSummary> data = menuItemService.getItemsByCategory(categoryId, sortBy, direction);
		return ResponseEntity.ok(ApiRes.success(data));
	}

}
