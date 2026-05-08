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
	public void validateUserToken(TokenValidRequest request, StreamObserver<ValidTokenResponse> responseObserver) {
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
	public void getUserDetail(UserDetailRequest request, StreamObserver<UserDetailResponse> responseObserver) {
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
	public void validateUser(UserValidRequest request, StreamObserver<UserValidResponse> responseObserver) {
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
	public void getUserDetails(UserDetailsRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
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
	public void deleteDeviceToken(DeviceTokenDeleteRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
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