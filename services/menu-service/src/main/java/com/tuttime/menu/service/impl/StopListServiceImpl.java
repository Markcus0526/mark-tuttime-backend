package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.menu.dto.StopListDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.StopList;
import com.tuttime.menu.grpc.client.RestaurantGrpcClient;
import com.tuttime.menu.mapper.StopListMapper;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.StopListRepository;
import com.tuttime.menu.service.StopListService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StopListServiceImpl implements StopListService {

    private final StopListRepository stopListRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final StopListMapper stopListMapper;
    private final RestaurantGrpcClient restaurantGrpcClient;

    @Override
    @Transactional
    @CacheEvict(value = "branch_menus", key = "#request.branchId()")
    public StopListDto.StopListResponse addToStopList(StopListDto.StopListRequest request, UUID currentUserId, String userRoles) {
        // 1. Verify item exists
        MenuItem menuItem = menuItemRepository.findById(request.menuItemId())
                .orElseThrow(() -> new InvalidRequestException("Menu item not found"));

        MenuCategory category = menuCategoryRepository.findById(menuItem.getCategory().getId())
                .orElseThrow(() -> new InvalidRequestException("Category not found"));

        restaurantGrpcClient.validateBranchPermission(category.getBranchId(), currentUserId, userRoles);

        // 2. Check if already on stop list for this branch to avoid duplicates
        stopListRepository.findByMenuItemIdAndBranchId(request.menuItemId(), request.branchId())
                .ifPresent(s -> {
                    throw new ConflictException("Item already on stop list");
                });

        // 3. Create Entity
        StopList stopList = StopList.builder()
                .menuItem(menuItem)
                .branchId(request.branchId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .build();

        StopList saved = stopListRepository.save(stopList);
        return stopListMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StopListDto.StopListResponse> getStopListByBranch(UUID branchId) {
        return stopListRepository.findByBranchId(branchId).stream()
                .map(stopListMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeFromStopList(UUID id, UUID currentUserId, String userRoles) {
        StopList stopList = stopListRepository.findById(id)
                .orElseThrow(() -> new InvalidRequestException("Stop list entry not found"));

        restaurantGrpcClient.validateBranchPermission(stopList.getBranchId(), currentUserId, userRoles);

        // Evict cache before deleting
        evictCache(stopList.getBranchId());

        stopListRepository.delete(stopList);
    }

    @Transactional(readOnly = true)
    @CacheEvict(value = "branch_menus", key = "#branchId")
    public void evictCache(UUID branchId) {
        // Helper for manual eviction
    }
}
