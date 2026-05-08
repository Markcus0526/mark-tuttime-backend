package com.tuttime.user.service;

import com.tuttime.common.enums.UserStatus;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.user.entity.*;
import com.tuttime.user.repository.UserDeviceRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.security.JwtTokenProvider;
import com.tuttime.user.service.TokenBlacklistService;
import io.grpc.Status;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.tuttime.common.event.util.ProtoUtil.convertOffsetDatetimeToProtobuf;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGrpcDelegate {

	private final UserRepository userRepository;
	private final UserDeviceRepository userDeviceRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final TokenBlacklistService tokenBlacklistService;

	@Transactional(readOnly = true)
	public ValidTokenResponse validateUserToken(TokenValidRequest request) {
		String token = request.getJwtToken();

		if (!jwtTokenProvider.validateAccessToken(token)) {
			throw Status.UNAUTHENTICATED.withDescription("Invalid Access Token").asRuntimeException();
		}

		Claims claims = jwtTokenProvider.getAccessClaims(token);
		String jti = claims.get("jti", String.class);
		String userIdStr = claims.get("userId", String.class);

		if (tokenBlacklistService.isBlacklisted(jti)) {
			throw Status.UNAUTHENTICATED.withDescription("Token has been revoked").asRuntimeException();
		}

		UUID userId = UUID.fromString(userIdStr);
		// Use eager fetch method
		User user = userRepository.findByIdWithRoles(userId)
				.orElseThrow(() -> Status.PERMISSION_DENIED
						.withDescription("User account is inactive, blocked, or not found")
						.asRuntimeException());

		return ValidTokenResponse.newBuilder()
				.setUserId(user.getId().toString())
				.setEmail(Optional.ofNullable(user.getEmail()).orElse(""))
				.setStatus(user.getStatus().name())
				.setRoles(combineUserRoles(user))
				.build();
	}

	@Transactional(readOnly = true)
	public UserDetailResponse getUserDetail(UserDetailRequest request) {
		UUID userId = UUID.fromString(request.getUserId());
		User user = userRepository.findByIdWithRoles(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found or deleted"));

		List<String> fcmTokens = userDeviceRepository.findByUserId(userId)
				.stream().map(UserDevice::getFcmToken).toList();

		UserDetailResponse.Builder builder = UserDetailResponse.newBuilder()
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
					.filter(Objects::nonNull)
					.map(Role::getName)
					.filter(Objects::nonNull)
					.collect(Collectors.joining(","));
			builder.setRoles(combinedRoles);
		}
		if (user.getAuthMethods() != null) {
			String authMethods = user.getAuthMethods().stream()
					.map(UserAuthMethod::getAuthType)
					.map(Enum::name)
					.collect(Collectors.joining(","));
			builder.setAuthMethods(authMethods);
		}
		if (!user.getLocations().isEmpty()) {
			SequencedSet<UserLocation> sequencedLocations = (SequencedSet<UserLocation>) user.getLocations();
			UserLocation lastLocation = sequencedLocations.getLast();
			builder.setLongitude(lastLocation.getLocation().getX());
			builder.setLatitude(lastLocation.getLocation().getY());
		}
		if (user.getLastLogin() != null) {
			builder.setLastLogin(convertOffsetDatetimeToProtobuf(user.getLastLogin()));
		}
		return builder.build();
	}

	@Transactional(readOnly = true)
	public UserValidResponse validateUser(UserValidRequest request) {
		UUID userId = UUID.fromString(request.getUserId());
		Optional<User> userOpt = userRepository.findByIdWithRoles(userId);
		if (userOpt.isPresent()) {
			User user = userOpt.get();
			return UserValidResponse.newBuilder()
					.setValid(user.getStatus() == UserStatus.ACTIVE)
					.setRoles(combineUserRoles(user))
					.build();
		}
		return UserValidResponse.newBuilder()
				.setValid(false)
				.setRoles("")
				.build();
	}

	@Transactional(readOnly = true)
	public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
		List<UUID> userIds = request.getUserIdsList().stream()
				.map(UUID::fromString).toList();

		List<User> users = userRepository.findAllByIdInWithRoles(userIds);
		Map<UUID, List<String>> userTokensMap = userDeviceRepository.findAllByUserIdIn(userIds)
				.stream()
				.collect(Collectors.groupingBy(
						UserDevice::getUserId,
						Collectors.mapping(UserDevice::getFcmToken, Collectors.toList())
				));

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
						.map(UserRole::getRole).filter(Objects::nonNull)
						.map(Role::getName).filter(Objects::nonNull)
						.collect(Collectors.joining(","));
				profile.setRoles(combinedRoles);
			}
			if (user.getAuthMethods() != null) {
				String authMethods = user.getAuthMethods().stream()
						.map(UserAuthMethod::getAuthType).map(Enum::name)
						.collect(Collectors.joining(","));
				profile.setAuthMethods(authMethods);
			}
			if (!user.getLocations().isEmpty()) {
				SequencedSet<UserLocation> sequencedLocations = (SequencedSet<UserLocation>) user.getLocations();
				UserLocation lastLocation = sequencedLocations.getLast();
				profile.setLongitude(lastLocation.getLocation().getX());
				profile.setLatitude(lastLocation.getLocation().getY());
			}
			if (user.getLastLogin() != null) {
				profile.setLastLogin(convertOffsetDatetimeToProtobuf(user.getLastLogin()));
			}
			responseBuilder.addProfiles(profile);
		}
		return responseBuilder.build();
	}

	@Transactional
	public void deleteDeviceToken(DeviceTokenDeleteRequest request) {
		userDeviceRepository.deleteByFcmToken(request.getFcmToken());
	}

	private String combineUserRoles(User user) {
		if (user.getRoles() == null || user.getRoles().isEmpty()) return "";
		return user.getRoles().stream()
				.map(userRole -> userRole.getRole().getName())
				.filter(Objects::nonNull)
				.collect(Collectors.joining(","));
	}
}