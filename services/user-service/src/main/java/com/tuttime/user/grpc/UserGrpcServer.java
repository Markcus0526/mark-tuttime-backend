package com.tuttime.user.grpc;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.*;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.security.JwtTokenProvider;
import com.tuttime.user.service.TokenBlacklistService;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.tuttime.common.util.DatetimeUtil.convertOffsetDatetimeToProtobuf;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcServer extends UserServiceGrpc.UserServiceImplBase {

	private final UserRepository userRepository;
	private final JwtTokenProvider jwtTokenProvider; // Your JWT utility
	private final TokenBlacklistService tokenBlacklistService;
	private final StringRedisTemplate redisTemplate; // For JTI blacklisting

	/**
	 * Used by API Gateway and other services to verify if a token is valid
	 * and retrieve the associated User Context (ID, Roles, Status).
	 */
	@Override
	@Transactional(readOnly = true) // Crucial for loading roles correctly
	public void validateUserToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
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
			ValidateTokenResponse response = ValidateTokenResponse.newBuilder()
					.setUserId(user.getId().toString())
					.setEmail(Optional.ofNullable(user.getEmail()).orElse(""))
					.setStatus(user.getStatus().name())
					.setRoles(combineUserRoles(user))
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Malformed token claims").asRuntimeException());
		} catch (StatusRuntimeException e) {
			responseObserver.onError(e);
		} catch (Exception e) {
			log.error("Token validation system error: ", e);
			// Do not leak e.getMessage() to the client for security reasons
			responseObserver.onError(Status.INTERNAL.withDescription("Authentication service error").asRuntimeException());
		}
	}

	/**
	 * Used by Review/Booking services to get public profile data.
	 */
	@Override
	@Transactional(readOnly = true)
	public void getUserProfile(UserByIdRequest request, StreamObserver<UserProfileResponse> responseObserver) {
		try {
			UUID userId = UUID.fromString(request.getUserId());

			// Fetching from DB (Ensure Repository handles the UUID correctly)
			User user = userRepository.findByIdAndDeletedFalse(userId)
					.orElseThrow(() -> new InvalidRequestException("User not found or deleted"));

			// Building Response based on your exact SQL fields
			UserProfileResponse.Builder responseBuilder = UserProfileResponse.newBuilder()
					.setUserId(user.getId().toString())
					.setEmail(user.getEmail())
					.setFullName(user.getFullName())
					.setStatus(user.getStatus().toString())
					.setCreatedAt(convertOffsetDatetimeToProtobuf(user.getCreatedAt()))
					.setUpdatedAt(convertOffsetDatetimeToProtobuf(user.getUpdatedAt()));
			if (user.getProfilePicture() != null) {
				responseBuilder.setRoles(user.getProfilePicture());
			}
			if (user.getLastLogin() != null) {
				responseBuilder.setLastLogin(convertOffsetDatetimeToProtobuf(user.getLastLogin()));
			}
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
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid UUID format").asRuntimeException());
		} catch (StatusRuntimeException e) {
			responseObserver.onError(e);
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL.asRuntimeException());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void validateUserById(UserByIdRequest request, StreamObserver<UserValidResponse> responseObserver) {
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
			// Only return an actual gRPC Error for bad input formats
			responseObserver.onError(Status.INVALID_ARGUMENT
					.withDescription("Invalid UUID format")
					.asRuntimeException());
		} catch (Exception e) {
			log.error("System error during user validation: ", e);
			responseObserver.onError(Status.INTERNAL.asRuntimeException());
		}
	}

	@Override
	public void getUserProfiles(UserByIdsRequest request, StreamObserver<UserProfilesResponse> responseObserver) {
		// 1. Extract and convert User IDs from Request
		List<UUID> userIds = request.getUserIdsList().stream()
				.map(UUID::fromString)
				.toList();

		// 2. Fetch users from Database
		List<User> users = userRepository.findAllByIdInAndDeletedFalse(userIds);

		// 3. Build the Response
		UserProfilesResponse.Builder responseBuilder = UserProfilesResponse.newBuilder();

		for (User user : users) {
			UserProfileResponse profile = UserProfileResponse.newBuilder()
					.setUserId(user.getId().toString())
					.setFullName(user.getFullName() != null ? user.getFullName() : "")
					.setPhoneNumber(user.getPhoneNumber() != null ? user.getPhoneNumber() : "")
					.setEmail(user.getEmail() != null ? user.getEmail() : "")
					.build();
			responseBuilder.addProfiles(profile);
		}

		// 4. Send response and close the stream
		responseObserver.onNext(responseBuilder.build());
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
