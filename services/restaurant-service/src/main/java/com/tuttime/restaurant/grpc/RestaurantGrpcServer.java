package com.tuttime.restaurant.grpc;

import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.service.RestaurantGrpcDelegate;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class RestaurantGrpcServer extends RestaurantServiceGrpc.RestaurantServiceImplBase {

	private final RestaurantGrpcDelegate delegate;

	@Override
	public void validateBranchPermission(BranchPermissionRequest request, StreamObserver<BranchPermissionResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.validateBranchPermission(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in validateBranchPermission", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getTableDetail(TableDetailRequest request, StreamObserver<TableDetailResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getTableDetail(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in getTableDetail", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getBranchSlots(BranchSlotsRequest request, StreamObserver<BranchSlotsResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getBranchSlots(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in getBranchSlots", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getBranchSchedule(ScheduleRequest request, StreamObserver<ScheduleResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getBranchSchedule(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in getBranchSchedule", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getStaffForUser(StaffUserRequest request, StreamObserver<StaffUserResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getStaffForUser(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in getStaffForUser", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getBranchDetail(BranchDetailRequest request, StreamObserver<BranchDetailResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getBranchDetail(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in getBranchDetail", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getTableHierarchyDetail(TableHierarchyRequest request, StreamObserver<TableHierarchyResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getTableHierarchyDetail(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in getTableHierarchyDetail", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void validateBranch(BranchValidRequest request, StreamObserver<BranchValidResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.validateBranch(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in validateBranch", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getBranchManagers(BranchManagersRequest request, StreamObserver<BranchManagersResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getBranchManagers(request));
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("Unexpected error in getBranchManagers", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void findAvailableTable(FindAvailableTableRequest request,
	                               StreamObserver<FindAvailableTableResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.findAvailableTable(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in findAvailableTable", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}
}