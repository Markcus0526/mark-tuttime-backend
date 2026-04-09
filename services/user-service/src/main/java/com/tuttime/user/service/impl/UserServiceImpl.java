package com.tuttime.user.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.exception.BadRequestException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.entity.enums.VerificationType;
import com.tuttime.user.kafka.UserSecurityProducer;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.*;
import com.tuttime.user.service.EmailService;
import com.tuttime.user.service.SmsService;
import com.tuttime.user.service.UserService;
import com.tuttime.user.util.TokenUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

	private final EmailService emailService;
	private final SmsService smsService;
	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final UserAuthMethodRepository authMethodRepository;
	private final VerificationCodeRepository verificationCodeRepository;
	private final UserLocationRepository userLocationRepository;
	private final UserMapper userMapper;
	private final PasswordEncoder passwordEncoder;
	private final UserSecurityProducer userSecurityProducer;
	private final TokenUtil tokenUtil;

	@Override
	public UserResponse createUser(UserCreateRequest request) {
		String identifier = request.getEmailOrPhone();
		String roleName = request.getRoleName();

		if (!StringUtils.hasText(identifier)) {
			throw new ResourceNotFoundException("Either phone number or email must be provided");
		}
		if (!StringUtils.hasText(roleName)) {
			throw new ResourceNotFoundException("Role must be provided");
		}

		String phoneNumber = null;
		String email =  null;
		if (identifier.contains("@"))
			email = identifier;
		else
			phoneNumber = identifier;

		// Check if user already exists by phone or email
		userRepository.findByPhoneNumberOrEmail(phoneNumber, email)
				.ifPresent(u -> {
					throw new ConflictException("Either phone number or email already in use");
				});

		// 1. Create and populate User first
		User user = new User();
		user.setPhoneNumber(phoneNumber);
		user.setEmail(email);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		user = userRepository.saveAndFlush(user);

		Role userRole = roleRepository.findByName(roleName)
				.orElseThrow(() -> new ResourceNotFoundException("Role " + roleName + " not found"));
		UserRole ur = new UserRole();
		ur.setUserId(user.getId());
		ur.setRole(userRole);
		userRoleRepository.save(ur);

		// 2. Create and link AuthMethod to the SAVED user
		String password = tokenUtil.generateRandomPassword();
		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUserId(user.getId()); // Now 'user' has an ID!
		authMethod.setCredential(passwordEncoder.encode(password));
		authMethod.setAuthType(StringUtils.hasText(phoneNumber) ? AuthProvider.PHONE_SMS : AuthProvider.EMAIL_PASS);
		authMethod.setVerified(false);

		user.addAuthMethod(authMethod);

		authMethodRepository.save(authMethod);
		user = userRepository.save(user);

		// Send verification (email or phone)
		// Generate code and save
		String code = tokenUtil.generateRandomCode();
		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(StringUtils.hasText(phoneNumber) ? VerificationType.PHONE_VERIFY : VerificationType.EMAIL_VERIFY);
		verificationCode.setTarget(StringUtils.hasText(phoneNumber) ? phoneNumber : email);
		verificationCode.setCodeHash(code);
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(3600)); // 5 min expiry
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		if (StringUtils.hasText(phoneNumber)) {
			smsService.sendVerificationCode(phoneNumber, code);
		} else {
			emailService.sendVerificationPasswordEmail(email, code, password);
		}

		return UserResponse.builder()
				.id(user.getId())
				.fullName(user.getFullName())
				.email(email)
				.phoneNumber(phoneNumber)
				.status(user.getStatus().name())
				.roles(ur.getRole().getName())
				.profilePicture(user.getProfilePicture())
				.lastLogin(user.getLastLogin())
				.build();
	}

	@Override
	public UserResponse getUserById(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		return userMapper.toResponse(user);
	}

	@Override
	public UserResponse updateUser(UUID id, UserUpdateRequest userUpdateRequest) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		if (userUpdateRequest.getPhoneNumber() != null && !userUpdateRequest.getPhoneNumber().equals(user.getPhoneNumber())) {
			userRepository.findByPhoneNumber(userUpdateRequest.getPhoneNumber())
					.ifPresent(u -> {
						throw new ConflictException("Phone number already in use");
					});
			user.setPhoneNumber(userUpdateRequest.getPhoneNumber());
		}
		if (userUpdateRequest.getEmail() != null && !userUpdateRequest.getEmail().equals(user.getEmail())) {
			userRepository.findByEmail(userUpdateRequest.getEmail())
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

		return UserResponse.builder()
				.id(user.getId())
				.fullName(user.getFullName())
				.email(user.getEmail())
				.phoneNumber(user.getPhoneNumber())
				.status(user.getStatus().name())
				.roles(user.getRoles().stream()
						.map(ur -> ur.getRole().getName())
						.collect(Collectors.joining(", ")))
				.profilePicture(user.getProfilePicture())
				.lastLogin(user.getLastLogin())
				.build();
	}

	@Override
	public void deleteUser(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		user.setStatus(UserStatus.DELETED);
		userRepository.save(user);

		// 2. Broadcast to Kafka
		// This tells the Gateway and Redis to block this ID permanently
		userSecurityProducer.sendUserDeleted(id.toString());

		log.info("User {} soft deleted and broadcasted to security topic", id);
	}

	@Override
	public Page<UserResponse> listUsers(
			UserStatus status,
			String roleNames,
			int page,
			int size,
			String sortBy,
			String direction) {
		Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
		PageRequest pageRequest = PageRequest.of(page, size, sortDirection, sortBy);

		return userRepository.findAllByOptionalStatusAndRole(status, roleNames, pageRequest)
				.map(user -> UserResponse.builder()
						.id(user.getId())
						.fullName(user.getFullName())
						.email(user.getEmail())
						.phoneNumber(user.getPhoneNumber())
						.status(user.getStatus().name())
						// Join all role names for the DTO field
						.roles(user.getRoles().stream()
								.map(ur -> ur.getRole().getName())
								.collect(Collectors.joining(", ")))
						.profilePicture(user.getProfilePicture())
						.lastLogin(user.getLastLogin())
						.createdAt(user.getCreatedAt())
						.updatedAt(user.getUpdatedAt())
						.build());
	}


	@Override
	public void assignRoleToUser(UUID userId, String roleName) {
		User user = userRepository.findById(userId)
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
		userRoleRepository.save(userRole);
	}

	@Override
	public void removeRoleFromUser(UUID userId, String roleName) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new ResourceNotFoundException("Role not found"));

		UserRole userRole = userRoleRepository.findByUserIdAndRole(user.getId(), role)
				.orElseThrow(() -> new ResourceNotFoundException("User role not found"));

		userRoleRepository.delete(userRole);
	}

	@Override
	public void addAuthMethod(UUID userId, AuthMethodCreateRequest authMethodCreateRequest) {
		User user = userRepository.findById(userId)
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
	public void updateUserLocation(UUID userId, UserLocationRequest userLocationRequest) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		UserLocation location = new UserLocation();
		location.setUserId(user.getId());
		location.setLocation(createGeographyPoint(userLocationRequest.getLatitude(), userLocationRequest.getLongitude()));
		location.setAccuracyMeters(userLocationRequest.getAccuracyMeters().floatValue());
		location.setRecordedAt(userLocationRequest.getRecordedAt() != null ? userLocationRequest.getRecordedAt() : OffsetDateTime.now());

		userLocationRepository.save(location);
	}

	@Transactional
	public UserResponse blockUser(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		user.setStatus(UserStatus.BLOCKED);
		userRepository.save(user);

		// Broadcast to Kafka topic so Gateway updates Redis immediately
		userSecurityProducer.sendUserBlocked(id.toString());

		return userMapper.toResponse(user);
	}

	@Transactional
	public UserResponse unblockUser(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);

		// Broadcast to Kafka to remove from Redis blocklist
		userSecurityProducer.sendUserUnblocked(id.toString());

		return userMapper.toResponse(user);
	}

	// Helper to create PostGIS geography point (depends on your JPA configuration)
	private Point createGeographyPoint(Double latitude, Double longitude) {
		// This depends on your PostGIS integration, e.g., using org.locationtech.jts.geom.Point or Hibernate Spatial
		// Example with JTS:
		// GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
		// return gf.createPoint(new Coordinate(longitude, latitude));
		throw new UnsupportedOperationException("Implement geography point creation based on your PostGIS setup");
	}
}