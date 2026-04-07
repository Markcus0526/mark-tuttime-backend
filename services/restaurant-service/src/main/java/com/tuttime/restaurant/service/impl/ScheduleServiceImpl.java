package com.tuttime.restaurant.service.impl;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.BranchSchedule;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import com.tuttime.restaurant.mapper.BranchScheduleMapper;
import com.tuttime.restaurant.mapper.RestaurantScheduleMapper;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.BranchScheduleRepository;
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
		List<RestaurantSchedule> schedules = restaurantScheduleRepository.findByRestaurantId(restaurantId);
		return schedules.stream()
				.map(restaurantScheduleMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public ScheduleResponse createScheduleForRestaurant(UUID restaurantId, ScheduleRequest request) {
		var restaurant = restaurantRepository.findById(restaurantId)
				.orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + restaurantId));

		RestaurantSchedule schedule = restaurantScheduleMapper.toEntity(request);
		schedule.setRestaurantId(restaurant.getRestaurantId());

		RestaurantSchedule saved = restaurantScheduleRepository.save(schedule);
		return restaurantScheduleMapper.toResponse(saved);
	}

	@Override
	@Transactional
	public ScheduleResponse updateRestaurantSchedule(UUID scheduleId, UUID restaurantId, ScheduleRequest request) {
		var schedule = restaurantScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

		if (!schedule.getRestaurantId().equals(restaurantId)) {
			throw new IllegalArgumentException("Schedule does not belong to the restaurant");
		}

		restaurantScheduleMapper.updateEntityFromRequest(request, schedule);
		RestaurantSchedule updated = restaurantScheduleRepository.save(schedule);
		return restaurantScheduleMapper.toResponse(updated);
	}

	@Override
	@Transactional
	public void deleteRestaurantSchedule(UUID scheduleId, UUID restaurantId) {
		var schedule = restaurantScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

		if (!schedule.getRestaurantId().equals(restaurantId)) {
			throw new IllegalArgumentException("Schedule does not belong to the restaurant");
		}

		restaurantScheduleRepository.delete(schedule);
	}

	///////////// Branch /////////////////
	@Override
	public List<ScheduleResponse> getSchedulesByBranchId(UUID branchId) {
		if (!branchRepository.existsById(branchId)) {
			throw new IllegalArgumentException("Branch not found: " + branchId);
		}
		List<BranchSchedule> schedules = branchScheduleRepository.findByBranchId(branchId);
		return schedules.stream()
				.map(branchScheduleMapper::toResponse)
				.collect(Collectors.toList());
	}

	@Override
	public ScheduleResponse createScheduleForBranch(UUID branchId, ScheduleRequest request) {
		var branch = branchRepository.findById(branchId)
				.orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));

		BranchSchedule schedule = branchScheduleMapper.toEntity(request);
		schedule.setBranchId(branch.getBranchId());

		BranchSchedule saved = branchScheduleRepository.save(schedule);
		return branchScheduleMapper.toResponse(saved);
	}

	@Override
	public ScheduleResponse updateBranchSchedule(UUID scheduleId, UUID branchId, ScheduleRequest request) {
		var schedule = branchScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

		if (!schedule.getBranchId().equals(branchId)) {
			throw new IllegalArgumentException("Schedule does not belong to the branch");
		}

		branchScheduleMapper.updateEntityFromRequest(request, schedule);
		BranchSchedule updated = branchScheduleRepository.save(schedule);
		return branchScheduleMapper.toResponse(updated);
	}

	@Override
	public void deleteBranchSchedule(UUID scheduleId, UUID branchId) {
		var schedule = branchScheduleRepository.findById(scheduleId)
				.orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

		if (!schedule.getBranchId().equals(branchId)) {
			throw new IllegalArgumentException("Schedule does not belong to the branch");
		}

		branchScheduleRepository.delete(schedule);
	}

}
