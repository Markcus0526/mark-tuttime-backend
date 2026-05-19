package com.tuttime.menu.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.service.ModifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    // ---------- Modifier Group ----------
    @GetMapping("/groups")
    @Operation(summary = "Get all modifier groups for a branch")
    public ResponseEntity<ApiRes<List<ModifierDto.ModifierGroupResponse>>> getGroupsByBranch(
            @RequestParam UUID branchId) {
        // Role check only; service will validate branch permission via gRPC
        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();
        List<ModifierDto.ModifierGroupResponse> groups =
                modifierService.getAllGroupsByBranch(branchId, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(groups));
    }

    @PostMapping("/groups")
    public ResponseEntity<ApiRes<ModifierDto.ModifierGroupResponse>> createGroup(
            @Valid @RequestBody ModifierDto.ModifierGroupRequest request) {
        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRes.success(modifierService.createGroup(request, currentUserId, userRoles)));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ApiRes<ModifierDto.ModifierGroupResponse>> getGroupById(@PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiRes.success(modifierService.getGroupById(groupId)));
    }

    @PutMapping("/groups/{groupId}")
    public ResponseEntity<ApiRes<ModifierDto.ModifierGroupResponse>> updateGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody ModifierDto.ModifierGroupRequest request) {
        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();
        return ResponseEntity.ok(ApiRes.success(
                modifierService.updateGroup(groupId, request, currentUserId, userRoles)));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiRes<Void>> deleteGroup(@PathVariable UUID groupId) {
        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();
        modifierService.deleteGroup(groupId, currentUserId, userRoles);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
    }

    // ---------- Modifier (options) ----------
    @GetMapping("/groups/{groupId}/modifiers")
    public ResponseEntity<ApiRes<List<ModifierDto.ModifierResponse>>> getModifiersByGroup(@PathVariable UUID groupId) {
        return ResponseEntity.ok(ApiRes.success(modifierService.getModifiersByGroupId(groupId)));
    }

    @PostMapping("/groups/{groupId}/modifiers")
    public ResponseEntity<ApiRes<ModifierDto.ModifierResponse>> addModifier(
            @PathVariable UUID groupId,
            @Valid @RequestBody ModifierDto.ModifierRequest request) {
        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRes.success(modifierService.addModifier(groupId, request, currentUserId, userRoles)));
    }

    @PutMapping("/groups/{groupId}/modifiers/{modifierId}")
    public ResponseEntity<ApiRes<ModifierDto.ModifierResponse>> updateModifier(
            @PathVariable UUID groupId,
            @PathVariable UUID modifierId,
            @Valid @RequestBody ModifierDto.ModifierRequest request) {
        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();
        return ResponseEntity.ok(
                ApiRes.success(modifierService.updateModifier(modifierId, request, currentUserId, userRoles)));
    }

    @DeleteMapping("/groups/{groupId}/modifiers/{modifierId}")
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
