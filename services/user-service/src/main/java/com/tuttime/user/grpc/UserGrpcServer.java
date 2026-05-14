package com.tuttime.user.grpc;

import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.user.service.UserGrpcDelegate;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcServer extends UserServiceGrpc.UserServiceImplBase {

	private final UserGrpcDelegate delegate;

	@Override
	public void validateUserToken(ValidateUserTokenRequest request, StreamObserver<ValidateUserTokenResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.validateUserToken(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getUserDetail(GetUserDetailRequest request, StreamObserver<GetUserDetailResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getUserDetail(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void validateUser(ValidateUserRequest request, StreamObserver<ValidateUserResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.validateUser(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void getUserDetails(GetUserDetailsRequest request, StreamObserver<GetUserDetailsResponse> responseObserver) {
		try {
			responseObserver.onNext(delegate.getUserDetails(request));
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			log.error("Unexpected error", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}

	@Override
	public void deleteDeviceToken(DeleteDeviceTokenRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
		try {
			delegate.deleteDeviceToken(request);
			responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
			responseObserver.onCompleted();
		} catch (Exception e) {
			log.error("Unexpected error", e);
			responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
		}
	}
}