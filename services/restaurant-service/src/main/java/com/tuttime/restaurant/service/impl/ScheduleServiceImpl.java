package com.tuttime.restaurant.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.BranchSchedule;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import com.tuttime.restaurant.mapper.BranchScheduleMapper;
import com.tuttime.restaurant.mapper.RestaurantScheduleMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.BranchScheduleRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.RestaurantScheduleRepository;
import com.tuttime.restaurant.service.RestaurantStaffService;
import com.tuttime.restaurant.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

	private final RestaurantStaffService restaurantStaffService;
	private final RestaurantScheduleRepository restaurantScheduleRepository;
	private final RestaurantRepository restaurantRepository;
	private final RestaurantScheduleMapper restaurantScheduleMapper;
	private final BranchScheduleRepository branchScheduleRepository;
	private final BranchRepository branchRepository;
	private final BranchScheduleMapper branchScheduleMapper;

	@Override
	public List<ScheduleResponse> getSchedulesByRestaurantId(UUID restaurantId) {
		if (!restaurantRepository.existsById(restaurantId)) {
			throw new IllegalArgumentException("Restaurant not found: " + restaurantId);
		}
		List<RestaurantSchedule> schedules = restaurantScheduleRepository.findAllByRestaurantIdAndDeletedFalse(restaurantId);
		return schedules.stream()
				.map(restaurantScheduleMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ScheduleResponse createScheduleForRestaurant(UUID restaurantId, ScheduleRequest request, UUID currentUserId, String userRoles) {
		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(restaurantId)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + restaurantId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		RestaurantSchedule schedule = restaurantScheduleMapper.toEntity(request);
		schedule.setRestaurantId(restaurant.getRestaurantId());

		RestaurantSchedule saved = restaurantScheduleRepository.save(schedule);
		return restaurantScheduleMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ScheduleResponse updateRestaurantSchedule(UUID scheduleId, ScheduleRequest request, UUID currentUserId, String userRoles) {
		RestaurantSchedule schedule = restaurantScheduleRepository.findByScheduleIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(schedule.getRestaurantId())
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + schedule.getRestaurantId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		restaurantScheduleMapper.updateEntityFromRequest(request, schedule);
		RestaurantSchedule updated = restaurantScheduleRepository.save(schedule);
		return restaurantScheduleMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void deleteRestaurantSchedule(UUID scheduleId, UUID currentUserId, String userRoles) {
		RestaurantSchedule schedule = restaurantScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

		Restaurant restaurant = restaurantRepository.findByRestaurantIdAndDeletedFalse(schedule.getRestaurantId())
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + schedule.getRestaurantId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(restaurant.getRestaurantId(), currentUserId);
		}
		if (!isAdmin && !isOwner) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		schedule.setDeleted(true);
		restaurantScheduleRepository.save(schedule);
	}

	@Override
	public void deleteAllByRestaurantId(UUID restaurantId) {
		restaurantScheduleRepository.deleteAllByRestaurantId(restaurantId);
	}

	///////////// Branch /////////////////
	@Override
	public List<ScheduleResponse> getSchedulesByBranchId(UUID branchId) {
		if (!branchRepository.existsById(branchId)) {
			throw new IllegalArgumentException("Branch not found: " + branchId);
		}
		List<BranchSchedule> schedules = branchScheduleRepository.findAllByBranchIdAndDeletedFalse(branchId);
		return schedules.stream()
				.map(branchScheduleMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	public ScheduleResponse createScheduleForBranch(UUID branchId, ScheduleRequest request, UUID currentUserId, String userRoles) {
		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(branchId)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + branchId));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		BranchSchedule schedule = branchScheduleMapper.toEntity(request);
		schedule.setBranchId(branch.getBranchId());

		BranchSchedule saved = branchScheduleRepository.save(schedule);
		return branchScheduleMapper.toResponse(saved);
	}

	@Override
	public ScheduleResponse updateBranchSchedule(UUID scheduleId, ScheduleRequest request, UUID currentUserId, String userRoles) {
		BranchSchedule schedule = branchScheduleRepository.findByScheduleIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(schedule.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + schedule.getBranchId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		branchScheduleMapper.updateEntityFromRequest(request, schedule);
		BranchSchedule updated = branchScheduleRepository.save(schedule);
		return branchScheduleMapper.toResponse(updated);
	}

	@Override
	public void deleteBranchSchedule(UUID scheduleId, UUID currentUserId, String userRoles) {
		BranchSchedule schedule = branchScheduleRepository.findByScheduleIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

		Branch branch = branchRepository.findByBranchIdAndDeletedFalse(schedule.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + schedule.getBranchId()));

		boolean isAdmin = userRoles.contains(UserRoleKind.ADMIN.name());
		boolean isOwner = false;
		boolean isManager = false;
		if (!isAdmin) {
			isOwner = restaurantStaffService.validateRestaurantOwner(branch.getRestaurantId(), currentUserId);
			if (!isOwner) isManager = restaurantStaffService.validateBranchManager(branch.getBranchId(), currentUserId);
		}
		if (!isAdmin && !isOwner && !isManager) {
			throw new AccessDeniedException("You do not have permission to manage this restaurant.");
		}

		schedule.setDeleted(true);
		branchScheduleRepository.save(schedule);
	}

	@Override
	public void deleteAllByBranchId(UUID branchId) {
		branchScheduleRepository.deleteAllByBranchId(branchId);
	}

}
