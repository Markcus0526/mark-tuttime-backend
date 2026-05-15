package com.tuttime.restaurant.grpc;

import com.google.protobuf.Empty;
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
	public void ping(Empty request, StreamObserver<Empty> responseObserver) {
		// No operation – just return empty
		responseObserver.onNext(Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	@Override
	public void validateBranchPermission(ValidateBranchPermissionRequest request, StreamObserver<ValidateBranchPermissionResponse> responseObserver) {
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
	public void getTableDetail(GetTableDetailRequest request, StreamObserver<GetTableDetailResponse> responseObserver) {
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
	public void getBranchSlots(GetBranchSlotsRequest request, StreamObserver<GetBranchSlotsResponse> responseObserver) {
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
	public void getBranchSchedule(GetBranchScheduleRequest request, StreamObserver<GetBranchScheduleResponse> responseObserver) {
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
	public void getStaffForUser(GetStaffForUserRequest request, StreamObserver<GetStaffForUserResponse> responseObserver) {
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
	public void getBranchDetail(GetBranchDetailRequest request, StreamObserver<GetBranchDetailResponse> responseObserver) {
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
	public void getTableHierarchyDetail(GetTableHierarchyDetailRequest request, StreamObserver<GetTableHierarchyDetailResponse> responseObserver) {
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
	public void validateBranch(ValidateBranchRequest request, StreamObserver<ValidateBranchResponse> responseObserver) {
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
	public void getBranchManagers(GetBranchManagersRequest request, StreamObserver<GetBranchManagersResponse> responseObserver) {
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

	@Override
	public void getBranchTotalTables(GetBranchTotalTablesRequest request,
	                                 StreamObserver<GetBranchTotalTablesResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getBranchTotalTables(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error in getBranchTotalTables", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}
}