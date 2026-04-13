package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.Schedule;
import com.tuttime.restaurant.mapper.ScheduleMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.ScheduleRepository;
import com.tuttime.restaurant.service.RestaurantStaffService;
import com.tuttime.restaurant.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

	private final RestaurantStaffService restaurantStaffService;
	private final ScheduleRepository scheduleRepository;
	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final ScheduleMapper scheduleMapper;

	@Override
	public List<ScheduleResponse> getSchedules(UUID entityId) {
		return scheduleRepository.findAllByEntityId(entityId)
				.stream()
				.map(scheduleMapper::toResponse)
				.toList(); // Simplified for Java 16+
	}

	@Override
	@Transactional
	public ScheduleResponse createSchedule(ScheduleRequest request, UUID currentUserId, String userRoles) {
		// 1. Validation & Permissions (Read-only)
		restaurantStaffService.validatePermissions(request.getEntityId(), currentUserId, userRoles);

		// 3. Database Save (Atomic transaction)
		Schedule schedule = scheduleMapper.toEntity(request);
		Schedule saved = scheduleRepository.save(schedule);
		return scheduleMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ScheduleResponse updateSchedule(UUID scheduleId, ScheduleRequest request, UUID currentUserId, String userRoles) {
		Schedule schedule = scheduleRepository.findByScheduleIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

		restaurantStaffService.validatePermissions(schedule.getEntityId(), currentUserId, userRoles);

		scheduleMapper.updateEntityFromRequest(request, schedule);
		Schedule updated = scheduleRepository.save(schedule);
		return scheduleMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void deleteSchedule(UUID scheduleId, UUID currentUserId, String userRoles) {
		Schedule schedule = scheduleRepository.findByScheduleIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + scheduleId));

		// 1. Centralized permission check
		restaurantStaffService.validatePermissions(schedule.getEntityId(), currentUserId, userRoles);

		// 2. Soft delete
		schedule.setDeleted(true);
		scheduleRepository.save(schedule);
	}

	@Override
	public void deleteAllByEntityId(UUID entityId) {
		scheduleRepository.deleteAllByEntityId(entityId);
	}
}
