package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.BranchSchedule;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import com.tuttime.restaurant.repository.BranchScheduleRepository;
import com.tuttime.restaurant.repository.RestaurantScheduleRepository;
import com.tuttime.restaurant.service.ScheduleService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@AllArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

	private final RestaurantScheduleRepository restaurantScheduleRepository;
	private final BranchScheduleRepository branchScheduleRepository;

	@Override
	public List<ScheduleResponse> getSchedulesByRestaurantId(UUID restaurantId) {
		List<RestaurantSchedule> schedules = restaurantScheduleRepository.findByRestaurantId(restaurantId);
		return schedules.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	@Override
	public ScheduleResponse createScheduleForRestaurant(UUID restaurantId, ScheduleRequest request) {
		RestaurantSchedule schedule = new RestaurantSchedule();
		schedule.setRestaurantId(restaurantId);
		updateScheduleEntityFromRequest(schedule, request);
		restaurantScheduleRepository.save(schedule);
		return mapToResponse(schedule);
	}

	@Override
	public List<ScheduleResponse> getSchedulesByBranchId(UUID branchId) {
		List<BranchSchedule> schedules = branchScheduleRepository.findByBranchId(branchId);
		return schedules.stream().map(this::mapToResponse).collect(Collectors.toList());
	}

	@Override
	public ScheduleResponse createScheduleForBranch(UUID branchId, ScheduleRequest request) {
		BranchSchedule schedule = new BranchSchedule();
		schedule.setBranchId(branchId);
		updateScheduleEntityFromRequest(schedule, request);
		branchScheduleRepository.save(schedule);
		return mapToResponse(schedule);
	}

	@Override
	public ScheduleResponse updateSchedule(UUID scheduleId, UUID restaurantId, UUID branchId, ScheduleRequest request) {
		if (restaurantId != null) {
			RestaurantSchedule schedule = restaurantScheduleRepository.findById(scheduleId)
					.filter(s -> s.getRestaurantId().equals(restaurantId))
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant schedule not found"));
			updateScheduleEntityFromRequest(schedule, request);
			restaurantScheduleRepository.save(schedule);
			return mapToResponse(schedule);
		} else if (branchId != null) {
			BranchSchedule schedule = branchScheduleRepository.findById(scheduleId)
					.filter(s -> s.getBranchId().equals(branchId))
                    .orElseThrow(() -> new ResourceNotFoundException("Branch schedule not found"));
			updateScheduleEntityFromRequest(schedule, request);
			branchScheduleRepository.save(schedule);
			return mapToResponse(schedule);
		} else {
			throw new IllegalArgumentException("Either restaurantId or branchId must be provided");
		}
	}

	@Override
	public void deleteSchedule(UUID scheduleId, UUID restaurantId, UUID branchId) {
		if (restaurantId != null) {
			RestaurantSchedule schedule = restaurantScheduleRepository.findById(scheduleId)
					.filter(s -> s.getRestaurantId().equals(restaurantId))
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant schedule not found"));
			restaurantScheduleRepository.delete(schedule);
		} else if (branchId != null) {
			BranchSchedule schedule = branchScheduleRepository.findById(scheduleId)
					.filter(s -> s.getBranchId().equals(branchId))
                    .orElseThrow(() -> new ResourceNotFoundException("Branch schedule not found"));
			branchScheduleRepository.delete(schedule);
		} else {
			throw new IllegalArgumentException("Either restaurantId or branchId must be provided");
		}
	}

	// --- Helpers ---

	private void updateScheduleEntityFromRequest(Object scheduleEntity, ScheduleRequest request) {
		if (scheduleEntity instanceof RestaurantSchedule) {
			RestaurantSchedule schedule = (RestaurantSchedule) scheduleEntity;
			schedule.setDayOfWeek(request.getDayOfWeek());
			schedule.setOpenTime(request.getOpenTime());
			schedule.setCloseTime(request.getCloseTime());
			schedule.setClosed(request.getIsClosed());
		} else if (scheduleEntity instanceof BranchSchedule) {
			BranchSchedule schedule = (BranchSchedule) scheduleEntity;
			schedule.setDayOfWeek(request.getDayOfWeek());
			schedule.setOpenTime(request.getOpenTime());
			schedule.setCloseTime(request.getCloseTime());
			schedule.setClosed(request.getIsClosed());
		}
	}

	private ScheduleResponse mapToResponse(Object scheduleEntity) {
		ScheduleResponse response = new ScheduleResponse();
		if (scheduleEntity instanceof RestaurantSchedule) {
			RestaurantSchedule schedule = (RestaurantSchedule) scheduleEntity;
			response.setScheduleId(schedule.getScheduleId());
			response.setParentId(schedule.getRestaurantId());
			response.setDayOfWeek(schedule.getDayOfWeek());
			response.setOpenTime(schedule.getOpenTime());
			response.setCloseTime(schedule.getCloseTime());
			response.setClosed(schedule.isClosed());
			response.setCreatedAt(schedule.getCreatedAt());
			response.setUpdatedAt(schedule.getUpdatedAt());
		} else if (scheduleEntity instanceof BranchSchedule) {
			BranchSchedule schedule = (BranchSchedule) scheduleEntity;
			response.setScheduleId(schedule.getScheduleId());
			response.setParentId(schedule.getBranchId());
			response.setDayOfWeek(schedule.getDayOfWeek());
			response.setOpenTime(schedule.getOpenTime());
			response.setCloseTime(schedule.getCloseTime());
			response.setClosed(schedule.isClosed());
			response.setCreatedAt(schedule.getCreatedAt());
			response.setUpdatedAt(schedule.getUpdatedAt());
		}
		return response;
	}
}
