package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.ScheduleDto;
import com.tuttime.restaurant.entity.Schedule;
import com.tuttime.restaurant.mapper.ScheduleMapper;
import com.tuttime.restaurant.repository.ScheduleRepository;
import com.tuttime.restaurant.service.ScheduleService;
import com.tuttime.restaurant.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

	private final StaffService staffService;
	private final ScheduleRepository scheduleRepository;
	private final ScheduleMapper scheduleMapper;

	@Override
	@Transactional(readOnly = true)
	public List<ScheduleDto.ScheduleResponse> getSchedules(UUID entityId) {
		return scheduleRepository.findAllByEntityId(entityId)
				.stream()
				.map(scheduleMapper::toResponse)
				.toList(); // Simplified for Java 16+
	}

	@Override
	@Transactional
	public ScheduleDto.ScheduleResponse createSchedule(ScheduleDto.ScheduleRequest request, UUID currentUserId, String userRoles) {
		// 1. Validation & Permissions (Read-only)
		staffService.validatePermissions(request.entityId(), currentUserId, userRoles);

		// 3. Database Save (Atomic transaction)
		Schedule schedule = scheduleMapper.toEntity(request);
		Schedule saved = scheduleRepository.save(schedule);
		return scheduleMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ScheduleDto.ScheduleResponse updateSchedule(UUID scheduleId, ScheduleDto.ScheduleRequest request, UUID currentUserId, String userRoles) {
		Schedule schedule = scheduleRepository.findByIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new InvalidRequestException("Schedule not found: " + scheduleId));

		staffService.validatePermissions(schedule.getEntityId(), currentUserId, userRoles);

		scheduleMapper.updateEntityFromRequest(request, schedule);
		Schedule updated = scheduleRepository.save(schedule);
		return scheduleMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void deleteSchedule(UUID scheduleId, UUID currentUserId, String userRoles) {
		Schedule schedule = scheduleRepository.findByIdAndDeletedFalse(scheduleId)
				.orElseThrow(() -> new InvalidRequestException("Schedule not found: " + scheduleId));

		// 1. Centralized permission check
		staffService.validatePermissions(schedule.getEntityId(), currentUserId, userRoles);

		// 2. Soft delete
		schedule.setDeleted(true);
		scheduleRepository.save(schedule);
	}

	@Override
	@Transactional
	public void deleteAllByEntityId(UUID entityId) {
		scheduleRepository.deleteAllByEntityId(entityId);
	}
}
