package com.tuttime.restaurant.service;

import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.grpc.client.UserGrpcClient;
import com.tuttime.restaurant.repository.*;
import com.tuttime.restaurant.repository.projection.BranchDetailProjection;
import com.tuttime.restaurant.repository.projection.ScheduleProjection;
import com.tuttime.restaurant.repository.projection.TableDetailProjection;
import com.tuttime.restaurant.repository.projection.TableHierarchyProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantGrpcDelegate {

	private final StaffRepository staffRepository;
	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final TableRepository tableRepository;
	private final ScheduleRepository scheduleRepository;
	private final UserGrpcClient userGrpClient;

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	@Transactional(readOnly = true)
	public BranchPermissionResponse validateBranchPermission(BranchPermissionRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		UUID currentUserId = UUID.fromString(request.getUserId());
		String userRoles = request.getUserRoles();

		boolean valid = false;

		// Validate user identity
		UserValidResponse userResponse = userGrpClient.validateUser(
				UserValidRequest.newBuilder().setUserId(currentUserId.toString()).build());

		if (userResponse.getValid()) {
			// Parse roles (comma-separated)
			List<String> roles = Arrays.asList(userRoles.split(","));
			if (roles.contains(UserRoleKind.ADMIN.name())) {
				valid = true;
			} else {
				if (roles.contains(UserRoleKind.OWNER.name())) {
					var branchOpt = branchRepository.findByIdAndDeletedFalse(branchId);
					if (branchOpt.isPresent()) {
						valid = staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
								branchOpt.get().getRestaurant().getId(), currentUserId, UserRoleKind.OWNER.name());
					}
				}
				if (!valid && roles.contains(UserRoleKind.MANAGER.name())) {
					valid = staffRepository.existsByBranchIdAndUserIdAndRoleName(
							branchId, currentUserId, UserRoleKind.MANAGER.name());
				}
			}
		}

		return BranchPermissionResponse.newBuilder().setValid(valid).build();
	}

	@Transactional(readOnly = true)
	public TableDetailResponse getTableDetail(TableDetailRequest request) {
		UUID tableId = UUID.fromString(request.getTableId());
		TableHierarchyProjection record = tableRepository.findTableHierarchy(tableId)
				.orElseThrow(() -> new InvalidRequestException("Table not found"));

		return TableDetailResponse.newBuilder()
				.setRestaurantId(record.getRestaurantId().toString())
				.setBranchId(record.getBranchId().toString())
				.setZoneId(record.getZoneId().toString())
				.setTableId(record.getTableId().toString())
				.setTableLabel(record.getTableLabel())
				.setBranchName(record.getBranchName())
				.setTotalTables(record.getTotalTables())
				.setAvailable(true)
				.build();
	}

	@Transactional(readOnly = true)
	public BranchSlotsResponse getBranchSlots(BranchSlotsRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int partySize = request.getPartySize();
		List<TableDetailProjection> tables = tableRepository.findTablesByBranchIdAndPartySize(
				branchId, partySize, TableStatus.ACTIVE);

		BranchSlotsResponse.Builder responseBuilder = BranchSlotsResponse.newBuilder();
		for (TableDetailProjection table : tables) {
			responseBuilder.addTables(TableSlotResponse.newBuilder()
					.setTableId(table.getTableId().toString())
					.setZoneId(table.getZoneId().toString())
					.setTableLabel(table.getTableLabel() != null ? table.getTableLabel() : "Standard")
					.setZoneName(table.getZoneName() != null ? table.getZoneName() : "Standard")
					.setMaxCapacity(table.getSeats())
					.build());
		}
		return responseBuilder.build();
	}

	@Transactional(readOnly = true)
	public ScheduleResponse getBranchSchedule(ScheduleRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int dayOfWeek = request.getDayOfWeek();
		Optional<ScheduleProjection> scheduleOpt = scheduleRepository.findActiveSchedule(branchId, dayOfWeek);

		if (scheduleOpt.isEmpty()) {
			return ScheduleResponse.newBuilder()
					.setClosed(true)
					.setOpenTime("00:00")
					.setCloseTime("00:00")
					.build();
		}

		ScheduleProjection schedule = scheduleOpt.get();
		return ScheduleResponse.newBuilder()
				.setOpenTime(schedule.getOpenTime().format(TIME_FORMATTER))
				.setCloseTime(schedule.getCloseTime().format(TIME_FORMATTER))
				.setClosed(schedule.getClosed())
				.build();
	}

	@Transactional(readOnly = true)
	public StaffUserResponse getStaffForUser(StaffUserRequest request) {
		UUID userId = UUID.fromString(request.getUserId());
		StaffUserResponse.Builder responseBuilder = StaffUserResponse.newBuilder();

		staffRepository.findByUserId(userId).ifPresent(staff -> {
			responseBuilder.setRestaurantId(staff.getRestaurant().getId().toString());
			responseBuilder.setActive(staff.getActive());

			if (staff.getBranch() != null && staff.getBranch().getId() != null) {
				UUID branchId = staff.getBranch().getId();
				responseBuilder.setBranchId(branchId.toString());
				branchRepository.findByIdAndDeletedFalse(branchId)
						.ifPresent(branch -> responseBuilder.setEntityName(branch.getName()));
			} else {
				restaurantRepository.findByIdAndDeletedFalse(staff.getRestaurant().getId())
						.ifPresent(restaurant -> responseBuilder.setEntityName(restaurant.getName()));
			}
		});
		return responseBuilder.build();
	}

	@Transactional(readOnly = true)
	public BranchDetailResponse getBranchDetail(BranchDetailRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		BranchDetailProjection details = branchRepository.findBranchDetailById(branchId)
				.orElseThrow(() -> new InvalidRequestException("Branch not found"));

		return BranchDetailResponse.newBuilder()
				.setBranchId(details.getBranchId().toString())
				.setRestaurantId(details.getRestaurantId().toString())
				.setBranchName(details.getBranchName())
				.setBranchAddress(details.getBranchAddress())
				.setBranchPhone(details.getBranchPhone() != null ? details.getBranchPhone() : "")
				.setRestaurantName(details.getRestaurantName())
				.build();
	}

	@Transactional(readOnly = true)
	public TableHierarchyResponse getTableHierarchyDetail(TableHierarchyRequest request) {
		UUID tableId = UUID.fromString(request.getTableId());
		TableHierarchyProjection record = tableRepository.findTableHierarchy(tableId)
				.orElseThrow(() -> new InvalidRequestException("Table not found"));

		return TableHierarchyResponse.newBuilder()
				.setTableId(record.getTableId().toString())
				.setZoneId(record.getZoneId().toString())
				.setBranchId(record.getBranchId().toString())
				.setRestaurantId(record.getRestaurantId().toString())
				.setTableLabel(record.getTableLabel())
				.setZoneName(record.getZoneName())
				.setBranchName(record.getBranchName())
				.setBranchAddress(record.getBranchAddress())
				.setBranchPhone(record.getBranchPhone())
				.setRestaurantName(record.getRestaurantName())
				.build();
	}

	@Transactional(readOnly = true)
	public BranchValidResponse validateBranch(BranchValidRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		Optional<com.tuttime.restaurant.entity.Branch> branchOpt = branchRepository.findById(branchId);
		if (branchOpt.isPresent()) {
			var branch = branchOpt.get();
			return BranchValidResponse.newBuilder()
					.setExists(true)
					.setName(branch.getName())
					.setActive(!branch.isDeleted())
					.build();
		}
		return BranchValidResponse.newBuilder().setExists(false).build();
	}

	@Transactional(readOnly = true)
	public BranchManagersResponse getBranchManagers(BranchManagersRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		List<String> managerIds = staffRepository.findManagerUserIdsByBranchId(branchId)
				.stream()
				.map(UUID::toString)
				.toList();
		return BranchManagersResponse.newBuilder()
				.addAllManagerIds(managerIds)
				.build();
	}

	@Transactional(readOnly = true)
	public FindAvailableTableResponse findAvailableTable(FindAvailableTableRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		UUID zoneId = request.hasZoneId() ? UUID.fromString(request.getZoneId()) : null;
		int partySize = request.getPartySize();

		List<DishTable> eligibleTables = tableRepository.findEligibleTables(branchId, zoneId, partySize);
		if (eligibleTables.isEmpty()) {
			return FindAvailableTableResponse.newBuilder().setAvailable(false).build();
		}
		DishTable table = eligibleTables.getFirst(); // pick the first suitable table
		return FindAvailableTableResponse.newBuilder()
				.setAvailable(true)
				.setTableId(table.getId().toString())
				.setZoneId(table.getZone().getId().toString())
				.setCapacity(table.getSeats())
				.setTableLabel(table.getLabel())
				.build();
	}

	@Transactional(readOnly = true)
	public GetBranchTotalTablesResponse getBranchTotalTables(GetBranchTotalTablesRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int total = tableRepository.countByBranchId(branchId);
		return GetBranchTotalTablesResponse.newBuilder().setTotalTables(total).build();
	}
}