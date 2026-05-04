package com.tuttime.user.grpc;

import com.tuttime.common.enums.UserStatus;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.user.entity.*;
import com.tuttime.user.repository.UserDeviceRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.security.JwtTokenProvider;
import com.tuttime.user.service.TokenBlacklistService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.tuttime.common.event.util.ProtoUtil.convertOffsetDatetimeToProtobuf;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcServer extends UserServiceGrpc.UserServiceImplBase {

	private final UserRepository userRepository;
	private final UserDeviceRepository userDeviceRepository;
	private final JwtTokenProvider jwtTokenProvider; // Your JWT utility
	private final TokenBlacklistService tokenBlacklistService;
	private final StringRedisTemplate redisTemplate; // For JTI blacklisting

	/**
	 * Used by API Gateway and other services to verify if a token is valid
	 * and retrieve the associated User Context (ID, Roles, Status).
	 */
	@Override
	@Transactional(readOnly = true) // Crucial for loading roles correctly
	public void validateUserToken(TokenValidRequest request, StreamObserver<ValidTokenResponse> responseObserver) {
		try {
			String token = request.getJwtToken();

			// 1. JWT Structural & Signature Validation
			if (!jwtTokenProvider.validateAccessToken(token)) {
				throw Status.UNAUTHENTICATED.withDescription("Invalid Access Token").asRuntimeException();
			}

			Claims claims = jwtTokenProvider.getAccessClaims(token);
			String jti = claims.get("jti", String.class);
			String userIdStr = claims.get("userId", String.class);

			// 2. Optimized Revocation Check
			// Consolidate 'tokenBlacklistService' and 'redisTemplate' into one service call
			if (tokenBlacklistService.isBlacklisted(jti)) {
				throw Status.UNAUTHENTICATED.withDescription("Token has been revoked").asRuntimeException();
			}

			// 3. Database Check: Use a projection or fetch-join for roles
			UUID userId = UUID.fromString(userIdStr);
			User user = userRepository.findByIdAndDeletedFalse(userId)
					.orElseThrow(() -> Status.PERMISSION_DENIED
							.withDescription("User account is inactive, blocked, or not found")
							.asRuntimeException());

			// 4. Build Response (Protobuf handles empty strings as default)
			ValidTokenResponse response = ValidTokenResponse.newBuilder()
					.setUserId(user.getId().toString())
					.setEmail(Optional.ofNullable(user.getEmail()).orElse(""))
					.setStatus(user.getStatus().name())
					.setRoles(combineUserRoles(user))
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

	/**
	 * Used by Review/Booking services to get public profile data.
	 */
	@Override
	@Transactional(readOnly = true)
	public void getUserDetail(UserDetailRequest request, StreamObserver<UserDetailResponse> responseObserver) {
		try {
			UUID userId = UUID.fromString(request.getUserId());

			// Fetching from DB (Ensure Repository handles the UUID correctly)
			User user = userRepository.findByIdAndDeletedFalse(userId)
					.orElseThrow(() -> new InvalidRequestException("User not found or deleted"));

			List<String> fcmTokens = userDeviceRepository.findByUserId(userId)
					.stream()
					.map(UserDevice::getFcmToken)
					.toList();

			// Building Response based on your exact SQL fields
			UserDetailResponse.Builder responseBuilder = UserDetailResponse.newBuilder()
					.setUserId(user.getId().toString())
					.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
					.setEmail(user.getEmail() != null ? user.getEmail() : "")
					.setFullName(user.getFullName() != null ? user.getFullName() : "")
					.setProfilePicture(user.getProfilePicture() != null ? user.getProfilePicture() : "")
					.setStatus(user.getStatus().toString())
					.addAllFcmTokens(fcmTokens)
					.setCreatedAt(convertOffsetDatetimeToProtobuf(user.getCreatedAt()))
					.setUpdatedAt(convertOffsetDatetimeToProtobuf(user.getUpdatedAt()));
			if (user.getRoles() != null && !user.getRoles().isEmpty()) {
				String combinedRoles = user.getRoles().stream()
						.map(UserRole::getRole)
						.filter(Objects::nonNull) // Ensure Role is not null
						.map(Role::getName)
						.filter(Objects::nonNull) // Ensure Role Name is not null
						.collect(Collectors.joining(","));

				responseBuilder.setRoles(combinedRoles);
			} else {
				responseBuilder.setRoles(""); // Protobuf builders hate nulls
			}
			if (user.getAuthMethods() != null) {
				String authMethods = user.getAuthMethods().stream()
						.map(UserAuthMethod::getAuthType)
						.map(Enum::name)
						.collect(Collectors.joining(","));

				responseBuilder.setAuthMethods(authMethods);
			}
			if (!user.getLocations().isEmpty()) {
				SequencedSet<UserLocation> sequencedLocations = (SequencedSet<UserLocation>) user.getLocations();
				UserLocation lastLocation = sequencedLocations.getLast();
				responseBuilder.setLongitude(lastLocation.getLocation().getX());
				responseBuilder.setLatitude(lastLocation.getLocation().getY());
			}
			// Null-safe mapping for last_login
			if (user.getLastLogin() != null) {
				responseBuilder.setLastLogin(convertOffsetDatetimeToProtobuf(user.getLastLogin()));
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
	public void validateUser(UserValidRequest request, StreamObserver<UserValidResponse> responseObserver) {
		try {
			UUID userId = UUID.fromString(request.getUserId());

			// Use ifPresentOrElse to handle both "Found" and "Not Found" as successful signals
			userRepository.findByIdAndDeletedFalse(userId).ifPresentOrElse(user -> {

				// User Found: Build response based on status and roles
				UserValidResponse response = UserValidResponse.newBuilder()
						.setValid(user.getStatus() == UserStatus.ACTIVE)
						.setRoles(combineUserRoles(user))
						.build();

				responseObserver.onNext(response);

			}, () -> {
				// User Not Found: Return valid = false (Protobuf default is false)
				responseObserver.onNext(UserValidResponse.newBuilder()
						.setValid(false)
						.setRoles("")
						.build());
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
	public void getUserDetails(UserDetailsRequest request, StreamObserver<UserDetailsResponse> responseObserver) {
		try {
			// 1. Extract and convert User IDs from Request
			List<UUID> userIds = request.getUserIdsList().stream()
					.map(UUID::fromString)
					.toList();

			// 2. Fetch users from Database
			List<User> users = userRepository.findAllByIdInAndDeletedFalse(userIds);

			Map<UUID, List<String>> userTokensMap = userDeviceRepository.findAllByUserIdIn(userIds)
					.stream()
					.collect(Collectors.groupingBy(
							UserDevice::getUserId,
							Collectors.mapping(UserDevice::getFcmToken, Collectors.toList())
					));

			// 3. Build the Response
			UserDetailsResponse.Builder responseBuilder = UserDetailsResponse.newBuilder();

			for (User user : users) {
				List<String> tokens = userTokensMap.getOrDefault(user.getId(), Collections.emptyList());

				UserDetailResponse.Builder profile = UserDetailResponse.newBuilder()
						.setUserId(user.getId().toString())
						.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
						.setEmail(user.getEmail() != null ? user.getEmail() : "")
						.setFullName(user.getFullName() != null ? user.getFullName() : "")
						.setProfilePicture(user.getProfilePicture() != null ? user.getProfilePicture() : "")
						.setStatus(user.getStatus().toString())
						.addAllFcmTokens(tokens)
						.setCreatedAt(convertOffsetDatetimeToProtobuf(user.getCreatedAt()))
						.setUpdatedAt(convertOffsetDatetimeToProtobuf(user.getUpdatedAt()));
				if (user.getRoles() != null && !user.getRoles().isEmpty()) {
					String combinedRoles = user.getRoles().stream()
							.map(UserRole::getRole)
							.filter(Objects::nonNull) // Ensure Role is not null
							.map(Role::getName)
							.filter(Objects::nonNull) // Ensure Role Name is not null
							.collect(Collectors.joining(","));

					profile.setRoles(combinedRoles);
				} else {
					profile.setRoles(""); // Protobuf builders hate nulls
				}
				if (user.getAuthMethods() != null) {
					String authMethods = user.getAuthMethods().stream()
							.map(UserAuthMethod::getAuthType)
							.map(Enum::name)
							.collect(Collectors.joining(","));

					profile.setAuthMethods(authMethods);
				}
				if (!user.getLocations().isEmpty()) {
					SequencedSet<UserLocation> sequencedLocations = (SequencedSet<UserLocation>) user.getLocations();
					UserLocation lastLocation = sequencedLocations.getLast();
					profile.setLongitude(lastLocation.getLocation().getX());
					profile.setLatitude(lastLocation.getLocation().getY());
				}
				// Null-safe mapping for last_login
				if (user.getLastLogin() != null) {
					profile.setLastLogin(convertOffsetDatetimeToProtobuf(user.getLastLogin()));
				}

				responseBuilder.addProfiles(profile);
			}

			// 4. Send response and close the stream
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
	public void deleteDeviceToken(DeviceTokenDeleteRequest request, StreamObserver<com.google.protobuf.Empty> responseObserver) {
		userDeviceRepository.deleteByFcmToken(request.getFcmToken());

		// Success confirmation
		responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
		responseObserver.onCompleted();
	}

	/**
	 * Helper to safely join roles into a comma-separated string
	 */
	private String combineUserRoles(User user) {
		if (user.getRoles() == null || user.getRoles().isEmpty()) {
			return "";
		}
		return user.getRoles().stream()
				.map(userRole -> userRole.getRole().getName())
				.filter(Objects::nonNull)
				.collect(Collectors.joining(","));
	}
}
