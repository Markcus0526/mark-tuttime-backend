package com.tuttime.user.grpc;

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

import java.util.Objects;
import java.util.SequencedSet;
import java.util.UUID;
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
	public void validateUser(ValidateUserRequest request, StreamObserver<UserContextResponse> responseObserver) {
		try {
			String token = request.getJwtToken();

			// 1. Validate specifically as an ACCESS TOKEN
			if (!jwtTokenProvider.validateAccessToken(token)) {
				throw new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Invalid Access Token"));
			}

			// 2. Parse claims (containing your userId, jti, roles)
			Claims claims = jwtTokenProvider.getAccessClaims(token);
			String jti = claims.get("jti", String.class);
			String userIdStr = claims.get("userId", String.class);
			// Inside UserGrpcServiceImpl.java
			if (tokenBlacklistService.isBlacklisted(jti)) {
				throw new StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("Token has been revoked"));
			}

			// 3. Redis Check: Is this specific JTI blacklisted (revoked session)?
			Boolean isRevoked = redisTemplate.hasKey("blacklist:jti:" + jti);
			if (isRevoked) {
				throw new StatusRuntimeException(Status.UNAUTHENTICATED
						.withDescription("Session has been revoked/logged out"));
			}

			// 4. Database Check: Fetch current status from the 'users' table
			UUID userId = UUID.fromString(userIdStr);
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND
							.withDescription("User record not found")));

			// 5. Business Logic: Blocked or Soft-Deleted users cannot pass
			if (user.getStatus() == UserStatus.BLOCKED) {
				throw new StatusRuntimeException(Status.PERMISSION_DENIED
						.withDescription("User account is blocked"));
			}
			if (user.getStatus() == UserStatus.DELETED) {
				throw new StatusRuntimeException(Status.NOT_FOUND
						.withDescription("User account has been deleted"));
			}

			// 6. Build Success Response
			UserContextResponse.Builder responseBuilder = UserContextResponse.newBuilder()
					.setUserId(user.getId().toString())
					.setEmail(user.getEmail() != null ? user.getEmail() : "")
					.setStatus(user.getStatus().name());

			if (user.getRoles() != null) {
				String combinedRoles = user.getRoles().stream()
						.map(UserRole::getRole)
						.map(Role::getName)
						.collect(Collectors.joining(","));

				responseBuilder.setRoles(combinedRoles);
			}

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid UserID format").asRuntimeException());
		} catch (StatusRuntimeException e) {
			responseObserver.onError(e);
		} catch (Exception e) {
			responseObserver.onError(Status.INTERNAL.withDescription("Validation error: " + e.getMessage()).asRuntimeException());
		}
	}

	/**
	 * Used by Review/Booking services to get public profile data.
	 */
	@Override
	@Transactional(readOnly = true)
	public void getUserProfile(GetUserRequest request, StreamObserver<UserProfileResponse> responseObserver) {
		try {
			UUID userId = UUID.fromString(request.getUserId());

			// Fetching from DB (Ensure Repository handles the UUID correctly)
			User user = userRepository.findById(userId)
					.filter(u -> u.getStatus() != UserStatus.DELETED) // PDF logic: Don't return soft-deleted users
					.orElseThrow(() -> new StatusRuntimeException(
							Status.NOT_FOUND.withDescription("User not found or deleted")));

			// Building Response based on your exact SQL fields
			UserProfileResponse.Builder responseBuilder = UserProfileResponse.newBuilder()
					.setId(user.getId().toString())
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
	public void getUserById(UserByIdRequest request, StreamObserver<UserResponse> responseObserver) {
		try {
			UUID userId = UUID.fromString(request.getUserId());

			// Fetching from DB (Ensure Repository handles the UUID correctly)
			User user = userRepository.findById(userId)
					.filter(u -> u.getStatus() != UserStatus.DELETED) // PDF logic: Don't return soft-deleted users
					.orElseThrow(() -> new StatusRuntimeException(
							Status.NOT_FOUND.withDescription("User not found or deleted")));

			// Building Response based on your exact SQL fields
			UserResponse.Builder responseBuilder = UserResponse.newBuilder()
					.setId(user.getId().toString())
					.setEmail(user.getEmail())
					.setFullName(user.getFullName())
					.setStatus(user.getStatus().toString())
					.setCreatedAt(convertOffsetDatetimeToProtobuf(user.getCreatedAt()))
					.setUpdatedAt(convertOffsetDatetimeToProtobuf(user.getUpdatedAt()));
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
}
