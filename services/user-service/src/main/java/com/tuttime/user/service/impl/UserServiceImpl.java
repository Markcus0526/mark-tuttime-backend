package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BadRequestException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.*;
import com.tuttime.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final UserAuthMethodRepository authMethodRepository;
	private final PhoneVerificationCodeRepository phoneVerificationCodeRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final UserLocationRepository userLocationRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	// Inject services/utilities for sending SMS/email etc.
	// private final NotificationService notificationService;

	@Override
	public UserResponse createUser(UserCreateRequest userCreateRequest) {
		// Check at least one contact exists
		if ((userCreateRequest.getPhoneNumber() == null || userCreateRequest.getPhoneNumber().isBlank())
				&& (userCreateRequest.getEmail() == null || userCreateRequest.getEmail().isBlank())) {
			throw new BadRequestException("Either phone number or email must be provided");
		}

		// Check uniqueness of phone and email
		userCreateRequest.getPhoneNumber();
		userCreateRequest.getEmail();
		userRepository.findByPhoneNumberAndIsDeletedFalse(userCreateRequest.getPhoneNumber())
				.ifPresent(u -> {
					throw new ConflictException("Phone number already in use");
				});
		userRepository.findByEmailAndIsDeletedFalse(userCreateRequest.getEmail())
				.ifPresent(u -> {
					throw new ConflictException("Email already in use");
				});

		User user = new User();
		user.setPhoneNumber(userCreateRequest.getPhoneNumber());
		user.setEmail(userCreateRequest.getEmail());
		user.setFullName(userCreateRequest.getFullName());
		user.setProfilePicture(userCreateRequest.getProfilePicture());
		user.setStatus(UserStatus.ACTIVE);
		user.setIsDeleted(false);
		user = userRepository.saveAndFlush(user);

		// Assign default role USER
		Role userRole = roleRepository.findByName("USER")
				.orElseThrow(() -> new ResourceNotFoundException("Default role USER not found"));
		UserRole ur = new UserRole();
		ur.setUserId(user.getId());
		ur.setRole(userRole);
		ur.setAssignedAt(OffsetDateTime.now());
		userRoleRepository.save(ur);

		return userMapper.toResponse(user);
	}

	@Override
	public UserResponse getUserById(UUID id) {
		User user = userRepository.findByIdAndIsDeletedFalse(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		return userMapper.toResponse(user);
	}

	@Override
	public UserResponse updateUser(UUID id, UserUpdateRequest userUpdateRequest) {
		User user = userRepository.findByIdAndIsDeletedFalse(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (userUpdateRequest.getPhoneNumber() != null && !userUpdateRequest.getPhoneNumber().equals(user.getPhoneNumber())) {
			userRepository.findByPhoneNumberAndIsDeletedFalse(userUpdateRequest.getPhoneNumber())
					.ifPresent(u -> {
						throw new ConflictException("Phone number already in use");
					});
			user.setPhoneNumber(userUpdateRequest.getPhoneNumber());
		}
		if (userUpdateRequest.getEmail() != null && !userUpdateRequest.getEmail().equals(user.getEmail())) {
			userRepository.findByEmailAndIsDeletedFalse(userUpdateRequest.getEmail())
					.ifPresent(u -> {
						throw new ConflictException("Email already in use");
					});
			user.setEmail(userUpdateRequest.getEmail());
		}
		if (userUpdateRequest.getFullName() != null) {
			user.setFullName(userUpdateRequest.getFullName());
		}
		if (userUpdateRequest.getProfilePicture() != null) {
			user.setProfilePicture(userUpdateRequest.getProfilePicture());
		}
		user = userRepository.save(user);
		return userMapper.toResponse(user);
	}

	@Override
	public void softDeleteUser(UUID id) {
		User user = userRepository.findByIdAndIsDeletedFalse(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		user.setIsDeleted(true);
		user.setStatus(UserStatus.DELETED);
		user.setDeletedAt(OffsetDateTime.now());
		userRepository.save(user);
	}

	@Override
	public Page<UserResponse> listUsers(boolean includeDeleted, int page, int size, String sortBy, String direction) {
		Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		PageRequest pageRequest = PageRequest.of(page, size, sortDirection, sortBy);

		Page<User> userPage;
		if (includeDeleted) {
			userPage = userRepository.findAll(pageRequest);  // include deleted users
		} else {
			userPage = userRepository.findByIsDeletedFalse(pageRequest);  // exclude deleted users
		}

		return userPage.map(userMapper::toResponse);
	}

	@Override
	public void assignRoleToUser(UUID userId, String roleName) {
		User user = userRepository.findByIdAndIsDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found"));

		boolean exists = userRoleRepository.existsByUserIdAndRole(user.getId(), role);
		if (exists) {
			throw new ConflictException("User already has this role");
		}

		UserRole userRole = new UserRole();
		userRole.setUserId(user.getId());
		userRole.setRole(role);
		userRole.setAssignedAt(OffsetDateTime.now());
		userRoleRepository.save(userRole);
	}

	@Override
	public void removeRoleFromUser(UUID userId, String roleName) {
		User user = userRepository.findByIdAndIsDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found"));

		UserRole userRole = userRoleRepository.findByUserIdAndRole(user.getId(), role)
				.orElseThrow(() -> new ResourceNotFoundException("User role not found"));

		userRoleRepository.delete(userRole);
	}

	@Override
	public void addAuthMethod(UUID userId, AuthMethodCreateRequest authMethodCreateRequest) {
		User user = userRepository.findByIdAndIsDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		authMethodRepository.findByUserIdAndAuthType(user.getId(), authMethodCreateRequest.getAuthType())
				.ifPresent(am -> {
					throw new ConflictException("Authentication method already exists for this user");
				});

		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUserId(user.getId());
		authMethod.setAuthType(authMethodCreateRequest.getAuthType());

		// For EMAIL_PASS, hash the password before storing
		if (authMethodCreateRequest.getAuthType() == AuthProvider.EMAIL_PASS) {
			authMethod.setCredential(passwordEncoder.encode(authMethodCreateRequest.getCredential()));
		} else {
			// For OAuth (GOOGLE, APPLE), store OAuth ID directly
			authMethod.setCredential(authMethodCreateRequest.getCredential());
		}

		authMethod.setVerified(false);
		authMethod.setCreatedAt(OffsetDateTime.now());

		authMethodRepository.save(authMethod);
	}

	@Override
	public void verifyPhoneCode(String phoneNumber, String code) {
		PhoneVerificationCode pvc = phoneVerificationCodeRepository
				.findFirstByPhoneNumberAndCodeAndUsedFalseAndExpiresAtAfter(phoneNumber, code, OffsetDateTime.now())
				.orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

		pvc.setUsed(true);
		phoneVerificationCodeRepository.save(pvc);

		// Optionally update user verified status or create user if needed
	}

	@Override
	public void requestPasswordReset(String emailOrPhone) {
		Optional<User> userOpt = Optional.empty();
		if (emailOrPhone.contains("@")) {
			userOpt = userRepository.findByEmailAndIsDeletedFalse(emailOrPhone);
		} else {
			userOpt = userRepository.findByPhoneNumberAndIsDeletedFalse(emailOrPhone);
		}

		if (userOpt.isEmpty()) {
			// Do not reveal user existence, silently return to avoid user enumeration
			return;
		}

		User user = userOpt.get();

		// Generate token
		String token = UUID.randomUUID().toString();
		PasswordResetToken prt = new PasswordResetToken();
		prt.setUserId(user.getId());
		prt.setToken(token);
		prt.setExpiresAt(OffsetDateTime.now().plusSeconds(3600)); // 1 hour expiration
		prt.setUsed(false);
		prt.setCreatedAt(OffsetDateTime.now());

		passwordResetTokenRepository.save(prt);

		// Send token via email or SMS (implement notification service)
		// notificationService.sendPasswordReset(user, token);
	}

	@Override
	public void resetPassword(String token, String newPassword) {
		PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
				.orElseThrow(() -> new BadRequestException("Invalid password reset token"));

		if (prt.isUsed() || prt.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new BadRequestException("Password reset token expired or already used");
		}

		UserAuthMethod emailPassAuth = authMethodRepository.findByUserIdAndAuthType(prt.getUserId(), AuthProvider.EMAIL_PASS)
				.orElseThrow(() -> new BadRequestException("User does not have password authentication"));

		emailPassAuth.setCredential(passwordEncoder.encode(newPassword));
		authMethodRepository.save(emailPassAuth);

		prt.setUsed(true);
		passwordResetTokenRepository.save(prt);
	}

	@Override
	public void updateUserLocation(UUID userId, UserLocationRequest userLocationRequest) {
		User user = userRepository.findByIdAndIsDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		UserLocation location = new UserLocation();
		location.setUserId(user.getId());
		location.setLocation(createGeographyPoint(userLocationRequest.getLatitude(), userLocationRequest.getLongitude()));
		location.setAccuracyMeters(userLocationRequest.getAccuracyMeters().floatValue());
		location.setRecordedAt(userLocationRequest.getRecordedAt() != null ? userLocationRequest.getRecordedAt() : OffsetDateTime.now());

		userLocationRepository.save(location);
	}
//
//	// Helper method to map User entity to UserResponse
//	private UserResponse mapToDto(User user) {
//		UserResponse dto = new UserResponse();
//		dto.setId(user.getId());
//		dto.setPhoneNumber(user.getPhoneNumber());
//		dto.setEmail(user.getEmail());
//		dto.setFullName(user.getFullName());
//		dto.setProfilePicture(user.getProfilePicture());
//		dto.setStatus(user.getStatus().name());
//		dto.setLastLogin(user.getLastLogin());
//		dto.setCreatedAt(user.getCreatedAt());
//		dto.setUpdatedAt(user.getUpdatedAt());
//		return dto;
//	}

	// Helper to create PostGIS geography point (depends on your JPA configuration)
	private Point createGeographyPoint(Double latitude, Double longitude) {
		// This depends on your PostGIS integration, e.g., using org.locationtech.jts.geom.Point or Hibernate Spatial
		// Example with JTS:
		// GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
		// return gf.createPoint(new Coordinate(longitude, latitude));
		throw new UnsupportedOperationException("Implement geography point creation based on your PostGIS setup");
	}

	private UserResponse toUserResponse(User user) {
		if (user == null) return null;
		return UserResponse.builder()
				.id(user.getId())
				.email(user.getEmail())
				.phoneNumber(user.getPhoneNumber())
				.fullName(user.getFullName())
				.profilePicture(user.getProfilePicture())
				.lastLogin((user.getLastLogin()))
				.build();
	}
}