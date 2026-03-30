package com.tuttime.restaurant.service.impl;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import com.tuttime.restaurant.mapper.RestaurantScheduleMapper;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.RestaurantScheduleRepository;
import com.tuttime.restaurant.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleServiceImpl implements ScheduleService {

	private final RestaurantScheduleRepository scheduleRepository;
	private final RestaurantRepository restaurantRepository;
	private final RestaurantScheduleMapper scheduleMapper;

	@Override
	public List<ScheduleResponse> getSchedulesByRestaurantId(UUID restaurantId) {
		if (!restaurantRepository.existsById(restaurantId)) {
			throw new IllegalArgumentException("Restaurant not found: " + restaurantId);
		}
		List<RestaurantSchedule> schedules = scheduleRepository.findByRestaurantId(restaurantId);
		return schedules.stream()
				.map(scheduleMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ScheduleResponse createScheduleForRestaurant(UUID restaurantId, ScheduleRequest request) {
		var restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));

		RestaurantSchedule schedule = scheduleMapper.toEntity(request);
		schedule.setRestaurantId(restaurant.getRestaurantId());

		RestaurantSchedule saved = scheduleRepository.save(schedule);
		return scheduleMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ScheduleResponse updateSchedule(UUID scheduleId, UUID restaurantId, Object unused, ScheduleRequest request) {
		var schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

		if (!schedule.getRestaurantId().equals(restaurantId)) {
			throw new IllegalArgumentException("Schedule does not belong to the restaurant");
		}

		scheduleMapper.updateEntityFromRequest(request, schedule);
		RestaurantSchedule updated = scheduleRepository.save(schedule);
		return scheduleMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void deleteSchedule(UUID scheduleId, UUID restaurantId, Object unused) {
		var schedule = scheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

		if (!schedule.getRestaurantId().equals(restaurantId)) {
			throw new IllegalArgumentException("Schedule does not belong to the restaurant");
		}

		scheduleRepository.delete(schedule);
	}

	@Override
	public List<ScheduleResponse> getSchedulesByBranchId(UUID branchId) {
		return List.of();
	}

	@Override
	public ScheduleResponse createScheduleForBranch(UUID branchId, ScheduleRequest request) {
		return null;
	}

	@Override
	public ScheduleResponse updateSchedule(UUID scheduleId, UUID restaurantId, String userId, ScheduleRequest request) {
		return null;
	}

	@Override
	public void deleteSchedule(UUID scheduleId, UUID restaurantId, String userId) {

	}

}
