package com.tuttime.restaurant.service;

import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.grpc.client.UserGrpcClient;
import com.tuttime.restaurant.repository.*;
import com.tuttime.restaurant.repository.record.BranchDetailRecord;
import com.tuttime.restaurant.repository.record.ScheduleRecord;
import com.tuttime.restaurant.repository.record.TableDetailRecord;
import com.tuttime.restaurant.repository.record.TableHierarchyRecord;
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
		TableHierarchyRecord record = tableRepository.findTableHierarchy(tableId)
				.orElseThrow(() -> new InvalidRequestException("Table not found"));

		return TableDetailResponse.newBuilder()
				.setRestaurantId(record.restaurantId().toString())
				.setBranchId(record.branchId().toString())
				.setZoneId(record.zoneId().toString())
				.setTableId(record.tableId().toString())
				.setTableLabel(record.tableLabel())
				.setBranchName(record.branchName())
				.setTotalTables(record.totalTables())
				.setAvailable(true)
				.build();
	}

	@Transactional(readOnly = true)
	public BranchSlotsResponse getBranchSlots(BranchSlotsRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int partySize = request.getPartySize();
		List<TableDetailRecord> tables = tableRepository.findTablesByBranchIdAndPartySize(
				branchId, partySize, TableStatus.ACTIVE);

		BranchSlotsResponse.Builder responseBuilder = BranchSlotsResponse.newBuilder();
		for (TableDetailRecord table : tables) {
			responseBuilder.addTables(TableSlotResponse.newBuilder()
					.setTableId(table.tableId().toString())
					.setZoneId(table.zoneId().toString())
					.setTableLabel(table.tableLabel() != null ? table.tableLabel() : "Standard")
					.setZoneName(table.zoneName() != null ? table.zoneName() : "Standard")
					.setMaxCapacity(table.seats())
					.build());
		}
		return responseBuilder.build();
	}

	@Transactional(readOnly = true)
	public ScheduleResponse getBranchSchedule(ScheduleRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int dayOfWeek = request.getDayOfWeek();
		Optional<ScheduleRecord> scheduleOpt = scheduleRepository.findActiveSchedule(branchId, dayOfWeek);

		if (scheduleOpt.isEmpty()) {
			return ScheduleResponse.newBuilder()
					.setClosed(true)
					.setOpenTime("00:00")
					.setCloseTime("00:00")
					.build();
		}

		ScheduleRecord schedule = scheduleOpt.get();
		return ScheduleResponse.newBuilder()
				.setOpenTime(schedule.openTime().format(TIME_FORMATTER))
				.setCloseTime(schedule.closeTime().format(TIME_FORMATTER))
				.setClosed(schedule.closed())
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
		BranchDetailRecord details = branchRepository.findBranchDetailById(branchId)
				.orElseThrow(() -> new InvalidRequestException("Branch not found"));

		return BranchDetailResponse.newBuilder()
				.setBranchId(details.branchId().toString())
				.setRestaurantId(details.restaurantId().toString())
				.setBranchName(details.branchName())
				.setBranchAddress(details.branchAddress())
				.setBranchPhone(details.branchPhone() != null ? details.branchPhone() : "")
				.setRestaurantName(details.restaurantName())
				.build();
	}

	@Transactional(readOnly = true)
	public TableHierarchyResponse getTableHierarchyDetail(TableHierarchyRequest request) {
		UUID tableId = UUID.fromString(request.getTableId());
		TableHierarchyRecord record = tableRepository.findTableHierarchy(tableId)
				.orElseThrow(() -> new InvalidRequestException("Table not found"));

		return TableHierarchyResponse.newBuilder()
				.setTableId(record.tableId().toString())
				.setZoneId(record.zoneId().toString())
				.setBranchId(record.branchId().toString())
				.setRestaurantId(record.restaurantId().toString())
				.setTableLabel(record.tableLabel())
				.setZoneName(record.zoneName())
				.setBranchName(record.branchName())
				.setBranchAddress(record.branchAddress())
				.setBranchPhone(record.branchPhone())
				.setRestaurantName(record.restaurantName())
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
}