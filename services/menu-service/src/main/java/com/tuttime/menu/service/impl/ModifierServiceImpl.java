package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.Modifier;
import com.tuttime.menu.entity.ModifierGroup;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.ModifierMapper;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.ModifierGroupRepository;
import com.tuttime.menu.repository.ModifierRepository;
import com.tuttime.menu.service.ModifierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModifierServiceImpl implements ModifierService {

    private final ModifierGroupRepository modifierGroupRepository;
    private final ModifierRepository modifierRepository;
    private final MenuItemRepository menuItemRepository;
    private final ModifierMapper modifierMapper;
    private final RestaurantGrpcClient restaurantGrpcClient;

    // --- Group Logic ---

    @Override
    @Transactional
    public ModifierDto.ModifierGroupResponse createGroup(ModifierDto.ModifierGroupRequest request,
                                                         UUID currentUserId, String userRoles) {
        // 1. Permission check for the branch
        restaurantGrpcClient.validateBranchPermission(request.branchId(), currentUserId, userRoles);

        // 2. Unique name within the branch
        if (modifierGroupRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalse(request.branchId(), request.name())) {
            throw new ConflictException("Modifier group with name '" + request.name() + "' already exists in this branch.");
        }

        // 3. Validate minSelect <= maxSelect
        if (request.minSelect() != null && request.maxSelect() != null && request.minSelect() > request.maxSelect()) {
            throw new InvalidRequestException("minSelect cannot be greater than maxSelect.");
        }

        ModifierGroup group = modifierMapper.toEntity(request);
        // branchId is mapped by MapStruct (field names match)
        group.setDeleted(false);
        group.setDeletedAt(null);
        ModifierGroup saved = modifierGroupRepository.save(group);
        return modifierMapper.toGroupResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ModifierDto.ModifierGroupResponse getGroupById(UUID id) {
        ModifierGroup group = modifierGroupRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new InvalidRequestException("Modifier group not found"));
        return modifierMapper.toGroupResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModifierDto.ModifierGroupResponse> getAllGroupsByBranch(UUID branchId, UUID currentUserId, String userRoles) {
        restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);
        List<ModifierGroup> groups = modifierGroupRepository.findByBranchIdAndDeletedFalseOrderByNameAsc(branchId);
        return groups.stream()
                .map(modifierMapper::toGroupResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", allEntries = true)  // evict cache on update
    public ModifierDto.ModifierGroupResponse updateGroup(UUID id, ModifierDto.ModifierGroupRequest request,
                                                         UUID currentUserId, String userRoles) {
        ModifierGroup group = modifierGroupRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new InvalidRequestException("Modifier group not found"));

        // 1. Permission check using the existing branch (branch cannot be changed)
        restaurantGrpcClient.validateBranchPermission(group.getBranchId(), currentUserId, userRoles);

        // 2. If the name is being changed, check uniqueness within the same branch
        if (!request.name().equals(group.getName())) {
            if (modifierGroupRepository.existsByBranchIdAndNameIgnoreCaseAndDeletedFalseAndIdNot(group.getBranchId(), request.name(), id)) {
                throw new ConflictException("Modifier group with name '" + request.name() + "' already exists in this branch.");
            }
            group.setName(request.name());
        }

        // 3. Update other fields
        group.setDescription(request.description());
        if (request.minSelect() != null) group.setMinSelect(request.minSelect());
        if (request.maxSelect() != null) group.setMaxSelect(request.maxSelect());

        // 4. Validate min <= max
        if (group.getMinSelect() > group.getMaxSelect()) {
            throw new InvalidRequestException("minSelect cannot be greater than maxSelect.");
        }

        return modifierMapper.toGroupResponse(modifierGroupRepository.save(group));
    }

    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", allEntries = true)
    public void deleteGroup(UUID id, UUID currentUserId, String userRoles) {
        ModifierGroup group = modifierGroupRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new InvalidRequestException("Modifier group not found"));
        // Permission check
        restaurantGrpcClient.validateBranchPermission(group.getBranchId(), currentUserId, userRoles);
        // Soft delete
        group.setDeleted(true);
        group.setDeletedAt(OffsetDateTime.now());
        modifierGroupRepository.save(group);
    }

    // --- Modifier Logic ---

    @Override
    @Transactional(readOnly = true)
    public List<ModifierDto.ModifierResponse> getModifiersByGroupId(UUID groupId) {
        // Check if group exists and is not deleted
        ModifierGroup group = modifierGroupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new InvalidRequestException("Modifier group not found"));
        List<Modifier> modifiers = modifierRepository.findByModifierGroupIdAndDeletedFalse(group.getId());
        return modifiers.stream()
                .map(modifierMapper::toModifierResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", allEntries = true)
    public ModifierDto.ModifierResponse addModifier(UUID groupId, ModifierDto.ModifierRequest request,
                                                    UUID currentUserId, String userRoles) {
        ModifierGroup group = modifierGroupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new InvalidRequestException("Modifier group not found"));
        // Permission check on the group's branch
        restaurantGrpcClient.validateBranchPermission(group.getBranchId(), currentUserId, userRoles);

        Modifier modifier = Modifier.builder()
                .modifierGroup(group)
                .name(request.name())
                .description(request.description())
                .priceAdjustment(request.priceAdjustment())
                .build();

        Modifier saved = modifierRepository.save(modifier);
        return modifierMapper.toModifierResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", allEntries = true)
    public ModifierDto.ModifierResponse updateModifier(UUID modifierId, ModifierDto.ModifierRequest request,
                                                       UUID currentUserId, String userRoles) {
        Modifier modifier = modifierRepository.findByIdAndDeletedFalse(modifierId)
                .orElseThrow(() -> new InvalidRequestException("Modifier not found"));
        // Get group and check permission
        ModifierGroup group = modifier.getModifierGroup();
        restaurantGrpcClient.validateBranchPermission(group.getBranchId(), currentUserId, userRoles);

        modifier.setName(request.name());
        modifier.setDescription(request.description());
        modifier.setPriceAdjustment(request.priceAdjustment());

        Modifier updated = modifierRepository.save(modifier);
        return modifierMapper.toModifierResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", allEntries = true)
    public void deleteModifier(UUID modifierId, UUID currentUserId, String userRoles) {
        Modifier modifier = modifierRepository.findByIdAndDeletedFalse(modifierId)
                .orElseThrow(() -> new InvalidRequestException("Modifier not found"));
        // Permission check via group
        ModifierGroup group = modifier.getModifierGroup();
        restaurantGrpcClient.validateBranchPermission(group.getBranchId(), currentUserId, userRoles);
        // Soft delete
        modifier.setDeleted(true);
        modifier.setDeletedAt(OffsetDateTime.now());
        modifierRepository.save(modifier);
    }

    // --- Linking to Menu Items ---
    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", allEntries = true)
    public void linkToItem(UUID groupId, UUID itemId, UUID currentUserId, String userRoles) {
        ModifierGroup group = modifierGroupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new InvalidRequestException("Modifier group not found"));
        MenuItem item = menuItemRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new InvalidRequestException("Menu item not found"));

        // Check permission on the group's branch (both group and item belong to same branch? Should validate)
        restaurantGrpcClient.validateBranchPermission(group.getBranchId(), currentUserId, userRoles);
        // Optionally verify item's branch matches group's branch (but item's branch is via category)
        item.getModifierGroups().add(group);
        menuItemRepository.save(item);
    }

    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", allEntries = true)
    public void unlinkFromItem(UUID groupId, UUID itemId, UUID currentUserId, String userRoles) {
        ModifierGroup group = modifierGroupRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new InvalidRequestException("Modifier group not found"));
        MenuItem item = menuItemRepository.findByIdAndDeletedFalse(groupId)
                .orElseThrow(() -> new InvalidRequestException("Menu item not found"));

        restaurantGrpcClient.validateBranchPermission(group.getBranchId(), currentUserId, userRoles);
        item.getModifierGroups().removeIf(g -> g.getId().equals(groupId));
        menuItemRepository.save(item);
    }

    // --- Deleted the problematic 'findGroups' method as it's not used and mismatched ---
}