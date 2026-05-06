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

import java.time.LocalTime;
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
		return scheduleRepository.findAllByEntityIdOrderByDayOfWeekAsc(entityId)
				.stream()
				.map(scheduleMapper::toResponse)
				.toList(); // Simplified for Java 16+
	}

	@Override
	@Transactional
	public ScheduleDto.ScheduleResponse createSchedule(ScheduleDto.ScheduleRequest request, UUID currentUserId, String userRoles) {
		// 1. Validation & Permissions (Read-only)
		staffService.validatePermissions(request.entityId(), currentUserId, userRoles);

		if (!request.closed()) {
			validateTiming(request);
			checkForOverlaps(request.entityId(), (int) request.dayOfWeek(), request.openTime(), request.closeTime(), null);
		}

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

		if (!request.closed()) {
			validateTiming(request);
			checkForOverlaps(request.entityId(), (int) request.dayOfWeek(), request.openTime(), request.closeTime(), scheduleId);
		}

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

	private void validateTiming(ScheduleDto.ScheduleRequest request) {
		if (request.openTime().isAfter(request.closeTime()) || request.openTime().equals(request.closeTime())) {
			throw new InvalidRequestException("Close time must be after open time. For overnight shifts, please split into two records (before and after midnight).");
		}
	}

	private void checkForOverlaps(UUID entityId, int dayOfWeek, java.time.LocalTime open, java.time.LocalTime close, UUID excludeId) {
		boolean isOverlapping = scheduleRepository.existsOverlappingSchedule(entityId, dayOfWeek, open, close, excludeId);
		if (isOverlapping) {
			throw new InvalidRequestException("Schedule overlaps with an existing time slot for day " + dayOfWeek);
		}
	}
}
