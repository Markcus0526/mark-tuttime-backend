package com.tuttime.restaurant.grpc;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.proto.*;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.grpc.client.UserGrpcClient;
import com.tuttime.restaurant.repository.BranchRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.repository.StaffRepository;
import com.tuttime.restaurant.repository.TableRepository;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService // Annotation from net.devh:grpc-server-spring-boot-starter
@RequiredArgsConstructor
public class RestaurantGrpcServer extends RestaurantServiceGrpc.RestaurantServiceImplBase {

	private final StaffRepository staffRepository;
	private final RestaurantRepository restaurantRepository;
	private final BranchRepository branchRepository;
	private final TableRepository tableRepository;
	private final UserGrpcClient userGrpClient;

	@PostConstruct
	public void init() {
		log.info("gRPC: RestaurantGrpcProvider initialized");
	}

	@Override
	public void validateBranchPermission(BranchPermissionRequest request, StreamObserver<BranchPermissionResponse> responseObserver) {
		boolean finalValid = false;
		try {
			UUID branchId = UUID.fromString(request.getBranchId());
			UUID currentUserId = UUID.fromString(request.getUserId());
			String userRoles = request.getUserRoles();

			// 1. Validate User Identity first
			UserValidResponse response = userGrpClient.validateUserById(
					UserByIdRequest.newBuilder().setUserId(currentUserId.toString()).build());

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
		} catch (Exception e) {
			log.error("Validation failed for entity: {}", request.getBranchId(), e);
			// finalValid remains false
		} finally {
			responseObserver.onNext(BranchPermissionResponse.newBuilder().setValid(finalValid).build());
			responseObserver.onCompleted();
		}
	}

	@Override
	public void checkAvailableTableForBooking(AvailableTableRequest request, StreamObserver<AvailableTableResponse> responseObserver) {
		UUID tableId = UUID.fromString(request.getTableId());
		int partSize = request.getPartySize();

		boolean available = tableRepository.findByIdAndDeletedFalse(tableId)
				.map(table ->
						table.getStatus() == TableStatus.ACTIVE && table.getSeats() <= partSize
				).orElse(false);

		responseObserver.onNext(AvailableTableResponse.newBuilder().setAvailable(available).build());
		responseObserver.onCompleted();
	}

	@Override
	public void getTablesForBooking(SlotAvailabilityRequest request, StreamObserver<TableListResponse> responseObserver) {
		UUID branchId = UUID.fromString(request.getBranchId());
		int partySize = request.getPartySize();

		// 1. Fetch tables from SQL that are active and can fit the party size
		List<Object[]> results = tableRepository.findTablesWithZoneName(branchId, partySize);

		// 2. Build the Protobuf response
		TableListResponse.Builder responseBuilder = TableListResponse.newBuilder();

		for (Object[] row : results) {
			TableInfo info = TableInfo.newBuilder()
					.setTableId(row[0].toString())
					.setTableLabel(row[1].toString())
					.setZoneName(row[3].toString()) // Accesses the Zone Name directly from the query
					.setMaxCapacity((Integer) row[2])
					.build();
			responseBuilder.addTables(info);
		}

		// 3. Send back to the Booking Service
		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void getStaffForUser(StaffUserRequest request, StreamObserver<StaffUserResponse> responseObserver) {
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
	}
}