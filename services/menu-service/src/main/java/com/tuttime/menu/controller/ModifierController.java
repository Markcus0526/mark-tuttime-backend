package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.service.ModifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu-modifiers")
@RequiredArgsConstructor
@Tag(name = "3.Menu Modifier", description = "Manage customization rules (e.g., 'Select Meat Temp')")
public class ModifierController {

	private final ModifierService modifierService;

	//---------- Modifier Group -------------

	@Operation(summary = "List modifier groups", description = "Lists modifier groups with search, filtering by restaurant, pagination, and sorting")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of modifier groups")
	})
	@GetMapping
	public ResponseEntity<ApiRes<Page<ModifierDto.ModifierGroupResponse>>> listAndSearchGroups(
			@Parameter(description = "Keyword to search by group name")
			@RequestParam(required = false) String query,

			@Parameter(description = "Filter by specific restaurant ID")
			@RequestParam(required = false) UUID restaurantId,

			@Parameter(description = "Page number (0-based)", required = false)
			@RequestParam(defaultValue = "0") int page,

			@Parameter(description = "Page size", required = false)
			@RequestParam(defaultValue = "20") int size,

			@Parameter(description = "Sort by field", required = false)
			@RequestParam(defaultValue = "id") String sort,

			@Parameter(description = "Sort direction: asc or desc", required = false)
			@RequestParam(defaultValue = "asc") String direction) {

		// Auth check based on your existing pattern
		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("Access denied"));
		}

		Sort sortOrder = direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending();
		Pageable pageable = PageRequest.of(page, size, sortOrder);

		// Call service with search criteria
		Page<ModifierDto.ModifierGroupResponse> groups = modifierService.findGroups(query, restaurantId, pageable);
		return ResponseEntity.ok(ApiRes.success(groups));
	}

	@PostMapping
	@Operation(summary = "Create a new modifier group")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Group created",
					content = @Content(schema = @Schema(implementation = ModifierDto.ModifierGroupResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload")
	})
	public ResponseEntity<ApiRes<ModifierDto.ModifierGroupResponse>> createGroup(
			@Valid @RequestBody ModifierDto.ModifierGroupRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ModifierDto.ModifierGroupResponse data = modifierService.createGroup(request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@GetMapping("/{groupId}")
	@Operation(summary = "Get group by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(schema = @Schema(implementation = ModifierDto.ModifierGroupResponse.class))),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<ApiRes<ModifierDto.ModifierGroupResponse>> getGroupById(@PathVariable UUID groupId) {

		ModifierDto.ModifierGroupResponse data = modifierService.getGroupById(groupId);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@PutMapping("/{groupId}")
	@Operation(summary = "Update an existing modifier group")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Group updated",
					content = @Content(schema = @Schema(implementation = ModifierDto.ModifierGroupResponse.class))),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<ApiRes<ModifierDto.ModifierGroupResponse>> updateGroup(
			@PathVariable UUID groupId,
			@Valid @RequestBody ModifierDto.ModifierGroupRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ModifierDto.ModifierGroupResponse data = modifierService.updateGroup(groupId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{groupId}")
	@Operation(summary = "Delete a modifier group")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Group deleted"),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<ApiRes<Void>> deleteGroup(@PathVariable UUID groupId) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		modifierService.deleteGroup(groupId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	//---------- Modifier -------------

	@GetMapping("/{groupId}/modifiers")
	@Operation(summary = "List all modifiers in a group", description = "Retrieves all individual modifier choices associated with a specific modifier group")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "List of modifiers"),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<ApiRes<List<ModifierDto.ModifierResponse>>> getModifiersByGroup(
			@PathVariable UUID groupId) {

		List<ModifierDto.ModifierResponse> modifiers = modifierService.getModifiersByGroupId(groupId);
		return ResponseEntity.ok(ApiRes.success(modifiers));
	}

	@PostMapping("/{groupId}/modifiers")
	@Operation(summary = "Add a specific modifier choice to a group")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Modifier added",
					content = @Content(schema = @Schema(implementation = ModifierDto.ModifierResponse.class))),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<ApiRes<ModifierDto.ModifierResponse>> addModifier(
			@PathVariable UUID groupId,
			@Valid @RequestBody ModifierDto.ModifierRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ModifierDto.ModifierResponse data = modifierService.addModifier(groupId, request, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@PutMapping("/{groupId}/modifiers/{modifierId}")
	@Operation(summary = "Update a specific modifier option")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Modifier updated",
					content = @Content(schema = @Schema(implementation = ModifierDto.ModifierResponse.class))),
			@ApiResponse(responseCode = "404", description = "Modifier not found")
	})
	public ResponseEntity<ApiRes<ModifierDto.ModifierResponse>> updateModifier(
			@PathVariable UUID groupId,
			@PathVariable UUID modifierId,
			@Valid @RequestBody ModifierDto.ModifierRequest request) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ModifierDto.ModifierResponse data = modifierService.updateModifier(modifierId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{groupId}/modifiers/{modifierId}")
	@Operation(summary = "Delete a specific modifier option")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Modifier deleted"),
			@ApiResponse(responseCode = "404", description = "Modifier not found")
	})
	public ResponseEntity<ApiRes<Void>> deleteModifier(
			@PathVariable UUID groupId,
			@PathVariable UUID modifierId) {

		validateAdminOrOwnerOrManager();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		modifierService.deleteModifier(modifierId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
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
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}
}
