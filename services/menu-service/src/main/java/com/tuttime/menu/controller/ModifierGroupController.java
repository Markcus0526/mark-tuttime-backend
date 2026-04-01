package com.tuttime.menu.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.dto.ModifierGroupDto;
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
@RequestMapping("/api/v1/menus/modifiers")
@RequiredArgsConstructor
@Tag(name = "Modifier Groups", description = "Manage customization rules (e.g., 'Select Meat Temp')")
public class ModifierGroupController {

	private final ModifierService modifierService;

	@PostMapping
	@Operation(summary = "Create a new modifier group")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Group created",
					content = @Content(schema = @Schema(implementation = ModifierGroupDto.ModifierGroupResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request payload")
	})
	public ResponseEntity<ApiRes<ModifierGroupDto.ModifierGroupResponse>> createGroup(
			@Valid @RequestBody ModifierGroupDto.ModifierGroupRequest request) {
		ModifierGroupDto.ModifierGroupResponse data = modifierService.createGroup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@GetMapping("/{groupId}")
	@Operation(summary = "Get group by ID")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Success",
					content = @Content(schema = @Schema(implementation = ModifierGroupDto.ModifierGroupResponse.class))),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<ApiRes<ModifierGroupDto.ModifierGroupResponse>> getGroupById(@PathVariable UUID groupId) {
		ModifierGroupDto.ModifierGroupResponse data = modifierService.getGroupById(groupId);
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
		ModifierDto.ModifierResponse data = modifierService.addModifierToGroup(groupId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
	}

	@DeleteMapping("/{groupId}")
	@Operation(summary = "Delete a modifier group")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Group deleted"),
			@ApiResponse(responseCode = "404", description = "Group not found")
	})
	public ResponseEntity<Void> deleteGroup(@PathVariable UUID groupId) {
		modifierService.deleteGroup(groupId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{groupId}/modifiers/{id}")
	@Operation(summary = "Delete a specific modifier option")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Modifier deleted"),
			@ApiResponse(responseCode = "404", description = "Modifier not found")
	})
	public ResponseEntity<Void> deleteModifier(@PathVariable UUID id) {
		modifierService.deleteModifier(id);
		return ResponseEntity.noContent().build();
	}
}
