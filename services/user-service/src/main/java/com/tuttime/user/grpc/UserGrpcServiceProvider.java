package com.tuttime.user.grpc;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.UserByIdRequest;
import com.tuttime.common.proto.UserResponse;
import com.tuttime.common.proto.UserServiceGrpc;
import com.tuttime.user.entity.*;
import com.tuttime.user.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.SequencedSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcServiceProvider extends UserServiceGrpc.UserServiceImplBase {

	private final UserRepository userRepository;

	@PostConstruct
	public void init() {
		log.info("gRPC: UserGrpcService initialized - SearchUsers method available");
	}

	@Override
	@Transactional(readOnly = true)
	public void getUserById(UserByIdRequest request, StreamObserver<UserResponse> responseObserver) {
		log.debug("gRPC: GetUserById request for userId: {}", request.getUserId());

		try {
			UUID userId = UUID.fromString(request.getUserId());
			User user = userRepository.findByIdAndIsDeletedFalse(userId)
					.orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

			UserResponse.Builder responseBuilder = UserResponse.newBuilder()
					.setUserId(user.getId().toString())
					.setEmail(user.getEmail())
					.setFullName(user.getFullName())
					.setStatus(user.getStatus().toString())
					.setCreatedAt(user.getCreatedAt().toEpochSecond())
					.setUpdatedAt(user.getUpdatedAt().toEpochSecond())
					.setIsDeleted(user.isDeleted());
			if (user.getProfilePicture() != null) {
				responseBuilder.setRoles(user.getProfilePicture());
			}
			if (user.getLastLogin() != null) {
				responseBuilder.setLastLogin(user.getLastLogin().toEpochSecond());
			}
			if (user.getRoles() != null) {
				String combinedRoles = user.getRoles().stream()
						.map(UserRole::getRole)
						.map(Role::getName)
						.collect(Collectors.joining(","));

				responseBuilder.setRoles(combinedRoles);
			}
			if (user.getAuthMethods() != null) {
				String authMethods = user.getAuthMethods().stream()
						.map(UserAuthMethod::getAuthType)
						.map(Enum::name)
						.collect(Collectors.joining(","));

				responseBuilder.setRoles(authMethods);
			}
			if (!user.getLocations().isEmpty()) {
				SequencedSet<UserLocation> sequencedLocations = (SequencedSet<UserLocation>) user.getLocations();
				UserLocation lastLocation = sequencedLocations.getLast();
				responseBuilder.setLongitude(lastLocation.getLocation().getX());
				responseBuilder.setLatitude(lastLocation.getLocation().getY());
			}

			UserResponse response = responseBuilder.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (ResourceNotFoundException e) {
			log.warn("gRPC: User not found: userId={}", request.getUserId());
			responseObserver.onError(Status.NOT_FOUND
					.withDescription("User not found: " + request.getUserId())
					.asRuntimeException());
		} catch (Exception e) {
			log.error("gRPC: Error getting user by ID: userId={}", request.getUserId(), e);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Failed to get user: " + e.getMessage())
					.withCause(e)
					.asRuntimeException());
		}
	}
}
