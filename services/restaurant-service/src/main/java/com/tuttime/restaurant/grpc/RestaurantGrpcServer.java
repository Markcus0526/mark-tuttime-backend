package com.tuttime.restaurant.grpc;

import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.grpc.client.UserGrpcClient;
import com.tuttime.restaurant.repository.*;
import com.tuttime.restaurant.repository.record.BranchDetailRecord;
import com.tuttime.restaurant.repository.record.ScheduleRecord;
import com.tuttime.restaurant.repository.record.TableDetailRecord;
import com.tuttime.restaurant.repository.record.TableHierarchyRecord;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@GrpcService // Annotation from net.devh:grpc-server-spring-boot-starter
@RequiredArgsConstructor
public class RestaurantGrpcServer extends RestaurantServiceGrpc.RestaurantServiceImplBase {

	private final StaffRepository staffRepository;
	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final TableRepository tableRepository;
	private final ScheduleRepository scheduleRepository;
	private final UserGrpcClient userGrpClient;

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

	@PostConstruct
	public void init() {
		log.info("gRPC: RestaurantGrpcProvider initialized");
	}

	@Override
	@Transactional(readOnly = true)
	public void validateBranchPermission(BranchPermissionRequest request, StreamObserver<BranchPermissionResponse> responseObserver) {
		boolean finalValid = false;
		try {
			UUID branchId = UUID.fromString(request.getBranchId());
			UUID currentUserId = UUID.fromString(request.getUserId());
			String userRoles = request.getUserRoles();

			// 1. Validate User Identity first
			UserValidResponse response = userGrpClient.validateUser(
					UserValidRequest.newBuilder().setUserId(currentUserId.toString()).build());

			if (response.getValid()) {
				// 2. Privilege Escalation: Admins are always valid
				if (userRoles.contains(UserRoleKind.ADMIN.name())) {
					finalValid = true;
				} else {
					if (userRoles.contains(UserRoleKind.OWNER.name())) {
						var branchOpt = branchRepository.findByIdAndDeletedFalse(branchId);
						if (branchOpt.isPresent()) {
							finalValid = staffRepository.existsByRestaurantIdAndUserIdAndRoleName(
									branchOpt.get().getRestaurant().getId(), currentUserId, UserRoleKind.OWNER.name());
						}
					}

					if (!finalValid && userRoles.contains(UserRoleKind.MANAGER.name())) {
						finalValid = staffRepository.existsByBranchIdAndUserIdAndRoleName(
								branchId, currentUserId, UserRoleKind.MANAGER.name());
					}
				}
			}

			responseObserver.onNext(BranchPermissionResponse.newBuilder().setValid(finalValid).build());
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getTableDetail(TableDetailRequest request, StreamObserver<TableDetailResponse> responseObserver) {
		try {
			UUID tableId = UUID.fromString(request.getTableId());

			TableHierarchyRecord record = tableRepository.findTableHierarchy(tableId)
					.orElseThrow(() -> new InvalidRequestException("Table not found"));

			TableDetailResponse response = TableDetailResponse.newBuilder()
					.setRestaurantId(record.restaurantId().toString())
					.setBranchId(record.branchId().toString())
					.setZoneId(record.zoneId().toString())
					.setTableId(record.tableId().toString())
					.setTableLabel(record.tableLabel())
					.setBranchName(record.branchName())
					.setTotalTables(record.totalTables())
					.setAvailable(true)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getBranchSlots(BranchSlotsRequest request, StreamObserver<BranchSlotsResponse> responseObserver) {
		try {
			UUID branchId = UUID.fromString(request.getBranchId());
			int partySize = request.getPartySize();

			// Fetch using the type-safe projection
			List<TableDetailRecord> tables = tableRepository.findTablesByBranchIdAndPartySize(
					branchId, partySize, TableStatus.ACTIVE);

			BranchSlotsResponse.Builder responseBuilder = BranchSlotsResponse.newBuilder();

			for (TableDetailRecord table : tables) {
				TableSlotResponse info = TableSlotResponse.newBuilder()
						.setTableId(table.tableId().toString())      // Record style: table.id() instead of getId()
						.setZoneId(table.zoneId().toString())      // Record style: table.id() instead of getId()
						.setTableLabel(table.tableLabel() != null ? table.tableLabel() : "Standard")
						.setZoneName(table.zoneName() != null ? table.zoneName() : "Standard")
						.setMaxCapacity(table.seats())
						.build();
				responseBuilder.addTables(info);
			}

			// 3. Send back to the Booking Service
			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getBranchSchedule(ScheduleRequest request, StreamObserver<ScheduleResponse> responseObserver) {
		try {
			UUID branchId = UUID.fromString(request.getBranchId());
			int dayOfWeek = request.getDayOfWeek();

			// 1. Fetch from DB
			Optional<ScheduleRecord> scheduleOpt = scheduleRepository.findActiveSchedule(branchId, dayOfWeek);

			ScheduleResponse.Builder responseBuilder = ScheduleResponse.newBuilder();

			if (scheduleOpt.isEmpty()) {
				// If no row exists for that day, the branch is closed
				responseBuilder.setClosed(true)
						.setOpenTime("00:00")
						.setCloseTime("00:00");
			} else {
				ScheduleRecord schedule = scheduleOpt.get();

				String openTimeStr = schedule.openTime().format(TIME_FORMATTER);
				String closeTimeStr = schedule.closeTime().format(TIME_FORMATTER);

				responseBuilder.setOpenTime(openTimeStr)
						.setCloseTime(closeTimeStr)
						.setClosed(schedule.closed());
			}

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getStaffForUser(StaffUserRequest request, StreamObserver<StaffUserResponse> responseObserver) {
		try {
			UUID userId = UUID.fromString(request.getUserId());

			staffRepository.findByUserId(userId).ifPresentOrElse(staff -> {
				StaffUserResponse.Builder responseBuilder = StaffUserResponse.newBuilder()
						.setRestaurantId(staff.getRestaurant().getId().toString());
				responseBuilder.setActive(staff.getActive());

				if (staff.getBranch() != null && staff.getBranch().getId() != null) {
					// Case 1: Fetch Branch Name
					UUID branchId = staff.getBranch().getId();
					responseBuilder.setBranchId(branchId.toString());

					branchRepository.findByIdAndDeletedFalse(branchId)
							.ifPresent(branch -> responseBuilder.setEntityName(branch.getName()));
				} else {
					// Case 2: Fetch Restaurant Name
					restaurantRepository.findByIdAndDeletedFalse(staff.getRestaurant().getId())
							.ifPresent(restaurant -> responseBuilder.setEntityName(restaurant.getName()));
				}

				responseObserver.onNext(responseBuilder.build());
			}, () -> {
				// Case 3: No staff record found - return empty response or error
				responseObserver.onNext(StaffUserResponse.newBuilder().build());
			});

			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getBranchDetail(BranchDetailRequest request, StreamObserver<BranchDetailResponse> responseObserver) {
		try {
			UUID branchId = UUID.fromString(request.getBranchId());

			BranchDetailRecord details = branchRepository.findBranchDetailById(branchId)
					.orElseThrow(() -> new InvalidRequestException("Branch not found"));

			// Map Record -> Protobuf Message
			BranchDetailResponse response = BranchDetailResponse.newBuilder()
					.setBranchId((details.branchId().toString()))
					.setRestaurantId(details.restaurantId().toString())
					.setBranchName(details.branchName())
					.setBranchAddress(details.branchAddress())
					.setBranchPhone(details.branchPhone() != null ? details.branchPhone() : "")
					.setRestaurantName(details.restaurantName())
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getTableHierarchyDetail(TableHierarchyRequest request, StreamObserver<TableHierarchyResponse> responseObserver) {
		try {
			UUID tableId = UUID.fromString(request.getTableId());

			TableHierarchyRecord record = tableRepository.findTableHierarchy(tableId)
					.orElseThrow(() -> new InvalidRequestException("Table not found"));

			TableHierarchyResponse response = TableHierarchyResponse.newBuilder()
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

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void validateBranch(BranchValidRequest request, StreamObserver<BranchValidResponse> responseObserver) {
		try {
			UUID branchId = UUID.fromString(request.getBranchId());

			// Query your branch table (assuming you have an 'active' status)
			BranchValidResponse response = branchRepository.findById(branchId)
					.map(branch -> BranchValidResponse.newBuilder()
							.setExists(true)
							.setName(branch.getName())
							.setActive(!branch.isDeleted()) // Business logic check
							.build())
					.orElse(BranchValidResponse.newBuilder()
							.setExists(false)
							.build());

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void getBranchManagers(BranchManagersRequest request, StreamObserver<BranchManagersResponse> responseObserver) {
		try {
			UUID branchId = UUID.fromString(request.getBranchId());

			// Correct lookup: find user_ids from the staffs table for this branch
			List<String> managerIds = staffRepository.findManagerUserIdsByBranchId(branchId)
					.stream()
					.map(UUID::toString)
					.toList();

			BranchManagersResponse response = BranchManagersResponse.newBuilder()
					.addAllManagerIds(managerIds)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}
}