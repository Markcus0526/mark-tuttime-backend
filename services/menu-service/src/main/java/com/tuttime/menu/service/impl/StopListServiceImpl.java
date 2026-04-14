package com.tuttime.menu.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.menu.dto.StopListDto;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.StopList;
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
@Transactional(readOnly = true)
public class StopListServiceImpl implements StopListService {

	private final StopListRepository stopListRepository;
	private final MenuItemRepository menuItemRepository;

	@Override
	@Transactional
	@CacheEvict(value = "branch_menus", key = "#request.branchId()")
	public StopListDto.StopListResponse addToStopList(StopListDto.StopListRequest request, UUID currentUserId, String userRoles) {
		// 1. Verify item exists
		MenuItem menuItem = menuItemRepository.findById(request.menuItemId())
				.orElseThrow(() -> new MethodArgumentNotValidException("Menu item not found"));

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
		return mapToResponse(saved);
	}

	@Override
	public List<StopListDto.StopListResponse> getStopListByBranch(UUID branchId) {
		return stopListRepository.findByBranchId(branchId).stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public void removeFromStopList(UUID id, UUID currentUserId, String userRoles) {
		StopList stopList = stopListRepository.findById(id)
				.orElseThrow(() -> new MethodArgumentNotValidException("Stop list entry not found"));

		// Evict cache before deleting
		evictCache(stopList.getBranchId());

		stopListRepository.delete(stopList);
	}

	@CacheEvict(value = "branch_menus", key = "#branchId")
	public void evictCache(UUID branchId) {
		// Helper for manual eviction
	}

	private StopListDto.StopListResponse mapToResponse(StopList s) {
		return new StopListDto.StopListResponse(
				s.getId(),
				s.getMenuItem().getId(),
				s.getMenuItem().getName(), // Crucial for Frontend UI
				s.getBranchId(),
				s.getStartDate(),
				s.getEndDate(),
				s.getReason()
		);
	}
}
