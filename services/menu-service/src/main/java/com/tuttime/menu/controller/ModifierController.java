package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.service.ModifierService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/menu-modifiers")
@RequiredArgsConstructor
@Tag(name = "3.Menu Modifier", description = "Manage customization rules (e.g., 'Select Meat Temp')")
public class ModifierController {

	private final ModifierService modifierService;

	@PostMapping
	@Operation(summary = "Create a new modifier group")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Group created",
					content = @Content(schema = @Schema(implementation = ModifierDto.ModifierGroupResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload")
	})
	public ResponseEntity<ApiRes<ModifierDto.ModifierGroupResponse>> createGroup(
			@Valid @RequestBody ModifierDto.ModifierGroupRequest request) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
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

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ModifierDto.ModifierGroupResponse data = modifierService.updateGroup(groupId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(data));
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

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
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

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		ModifierDto.ModifierResponse data = modifierService.updateModifier(modifierId, request, currentUserId, userRoles);
		return ResponseEntity.ok(ApiRes.success(data));
	}

	@DeleteMapping("/{groupId}")
	@Operation(summary = "Delete a modifier group")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Group deleted"),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<ApiRes<Void>> deleteGroup(@PathVariable UUID groupId) {

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		modifierService.deleteGroup(groupId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
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

		if (!UserContext.isValidUUID() || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiRes.error("You do not manage this specific restaurant"));
		}
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
		String userRoles = UserContext.getRoles();

		modifierService.deleteModifier(modifierId, currentUserId, userRoles);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}
}
