package com.tuttime.user.service.impl;

import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.enums.UserStatus;
import com.tuttime.common.enums.UserStatusEvent;
import com.tuttime.common.enums.VerificationType;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.event.UserEvent;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.provider.StorageProvider;
import com.tuttime.user.provider.TokenProvider;
import com.tuttime.user.repository.*;
import com.tuttime.user.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final UserAuthMethodRepository authMethodRepository;
	private final VerificationCodeRepository verificationCodeRepository;
	private final UserLocationRepository userLocationRepository;
	private final UserMapper userMapper;
	private final StorageProvider storageProvider;
	private final PasswordEncoder passwordEncoder;
	private final EventProducer eventProducer;
	private final TokenProvider tokenProvider;

	@Override
	@Transactional
	public UserDto.UserResponse createUser(UserDto.UserCreateRequest request) {
		String identifier = request.emailOrPhone();
		String roleName = request.roleName();

		// 1. Basic Validation
		if (!StringUtils.hasText(identifier)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}

		String email = identifier.contains("@") ? identifier : null;
		String phoneNumber = (email == null) ? identifier : null;

		// 2. Conflict Check
		userRepository.findByPhoneNumberOrEmail(phoneNumber, email)
				.ifPresent(u -> {
					throw new ConflictException("Phone number or email already in use");
				});

		// 3. Persist User Entity
		User user = new User();
		user.setPhoneNumber(phoneNumber);
		user.setEmail(email);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		user.setLanguage(StringUtils.hasText(request.language()) ? request.language() : "en");
		user = userRepository.saveAndFlush(user);

		// 4. Assign Role
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new InvalidRequestException("Role " + roleName + " not found"));
		user.getRoles().add(UserRole.builder().user(user).role(role).build());

		// 5. Setup Password & Auth Method
		String rawPassword = tokenProvider.generateRandomPassword();
		UserAuthMethod authMethod = UserAuthMethod.builder()
				.user(user)
				.credential(passwordEncoder.encode(rawPassword))
				.authType(email != null ? AuthProvider.EMAIL_PASS : AuthProvider.PHONE_SMS)
				.verified(false)
				.build();
		user.addAuthMethod(authMethod);
		authMethodRepository.save(authMethod);

		// 6. Generate Verification Code
		String code = tokenProvider.generateRandomCode();
		VerificationType type = (phoneNumber != null) ? VerificationType.PHONE_VERIFY : VerificationType.EMAIL_VERIFY;

		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(type);
		verificationCode.setTarget(identifier);
		verificationCode.setCodeHash(passwordEncoder.encode(code));
		verificationCode.setExpiresAt(OffsetDateTime.now().plusHours(1)); // Extended for manual creation
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		// 7. Fire Kafka Event to Notification Service
		UserEvent.VerificationEvent event = UserEvent.VerificationEvent.builder()
				.userId(user.getId())
				.email(email)
				.phoneNumber(phoneNumber)
				.type(type)
				.templateKey(NotificationTemplateKey.ADMIN_USER_CREATED)
				.build();

		eventProducer.sendUserVerificationRequestedEvent(event);

		return userMapper.toResponse(userRepository.save(user));
	}

	@Override
	@Transactional(readOnly = true)
	public UserDto.UserResponse getUserById(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("User not found"));
		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public UserDto.UserResponse updateUser(UUID id, UserDto.UserUpdateRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		boolean changed = false;

		if (request.phoneNumber() != null && !request.phoneNumber().equals(user.getPhoneNumber())) {
			userRepository.findByPhoneNumberAndDeletedFalse(request.phoneNumber())
					.ifPresent(u -> {
						throw new ConflictException("Phone number already in use");
					});
			user.setPhoneNumber(request.phoneNumber());
			changed = true;
		}

		if (request.email() != null && !request.email().equals(user.getEmail())) {
			userRepository.findByEmailAndDeletedFalse(request.email())
					.ifPresent(u -> {
						throw new ConflictException("Email already in use");
					});
			user.setEmail(request.email());
			changed = true;
		}

		if (request.fullName() != null && !request.fullName().equals(user.getFullName())) {
			user.setFullName(request.fullName());
			changed = true;
		}

		if (request.profilePicture() != null && !request.profilePicture().equals(user.getProfilePicture())) {
			user.setProfilePicture(request.profilePicture());
			changed = true;
		}

		if (request.language() != null && !request.language().equals(user.getLanguage())) {
			user.setLanguage(request.language());
			changed = true;
		}

		if (changed) {
			user = userRepository.save(user);
			UserEvent.ProfileUpdatedEvent event = UserEvent.ProfileUpdatedEvent.builder()
					.userId(user.getId())
					.fullName(user.getFullName())
					.profilePicture(user.getProfilePicture())
					.language(user.getLanguage())
					.updatedAt(user.getUpdatedAt())
					.build();
			eventProducer.sendUserUpdatedEvent(event);
		}

		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public UserDto.UserResponse updateMe(UUID id, UserDto.UserUpdateRequest request, MultipartFile file) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		// 1. Handle profile picture: file takes precedence
		if (file != null && !file.isEmpty()) {
			if (StringUtils.hasText(user.getProfilePicture())) {
				storageProvider.delete(user.getProfilePicture());
			}
			String key = storageProvider.store(file);
			user.setProfilePicture(key);
		} else if (request != null && request.profilePicture() != null) {
			// Only set from JSON if no file and request is present
			user.setProfilePicture(request.profilePicture());
		}

		// 2. Handle other fields only if request is not null
		if (request != null) {
			if (request.phoneNumber() != null && !request.phoneNumber().equals(user.getPhoneNumber())) {
				userRepository.findByPhoneNumberAndDeletedFalse(request.phoneNumber())
						.ifPresent(u -> {
							throw new ConflictException("Phone number already in use");
						});
				user.setPhoneNumber(request.phoneNumber());
			}
			if (request.email() != null && !request.email().equals(user.getEmail())) {
				userRepository.findByEmailAndDeletedFalse(request.email())
						.ifPresent(u -> {
							throw new ConflictException("Email already in use");
						});
				user.setEmail(request.email());
			}
			if (request.fullName() != null) {
				user.setFullName(request.fullName());
			}
			if (request.language() != null) {
				user.setLanguage(request.language());
			}
		}

		user = userRepository.save(user);

		UserEvent.ProfileUpdatedEvent event = UserEvent.ProfileUpdatedEvent.builder()
				.userId(user.getId())
				.fullName(user.getFullName())
				.profilePicture(user.getProfilePicture())
				.language(user.getLanguage())
				.updatedAt(user.getUpdatedAt())
				.build();
		eventProducer.sendUserUpdatedEvent(event);

		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public void deleteUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));
		user.setDeleted(true);
		userRepository.save(user);

		// 2. Broadcast to Kafka
		// We include the 'password' in params so the user knows how to log in
		Map<String, Object> params = new HashMap<>();
		params.put("phoneNumber", user.getPhoneNumber());
		params.put("email", user.getEmail());

		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.userId(userId)
				.type(UserStatusEvent.USER_DELETED)
				.jti(null)
				.ttlMillis(0)
				.templateKey(NotificationTemplateKey.ADMIN_USER_DELETED)
				.parameters(params)
				.build();
		eventProducer.sendUserSecurityEvent(event);

		log.info("User {} soft deleted and broadcasted to security topic", userId);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<UserDto.UserResponse> findUsers(String query, UserStatus status, String roleName, Pageable pageable) {
		// Build the dynamic WHERE clause
		Specification<User> spec = (root, criteriaQuery, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Add Keyword Search (Name, Email, or Phone)
			if (query != null && !query.isBlank()) {
				String pattern = "%" + query.toLowerCase() + "%";
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("fullName")), pattern),
						cb.like(cb.lower(root.get("email")), pattern),
						cb.like(cb.lower(root.get("phoneNumber")), pattern)
				));
			}

			// Always filter out deleted users (Strict Boolean Check)
			predicates.add(cb.equal(root.get("deleted"), false));

			// Add Status Filter
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}

			// Add Role Filter (joining roles table)
			if (roleName != null && !roleName.isBlank()) {
				predicates.add(cb.equal(root.join("roles").join("role").get("name"), roleName.toUpperCase()));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		return userRepository.findAll(spec, pageable).map(userMapper::toResponse);
	}

	@Override
	@Transactional
	public void assignRoleToUser(UUID userId, String roleName) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new InvalidRequestException("Role not found"));

		boolean exists = userRoleRepository.existsByUserIdAndRole(user.getId(), role);
		if (exists) {
			throw new ConflictException("User already has this role");
		}

		UserRole userRole = UserRole.builder()
				.user(user)
				.role(role)
				.build();
		userRoleRepository.save(userRole);
	}

	@Override
	@Transactional
	public void removeRoleFromUser(UUID userId, String roleName) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new InvalidRequestException("Role not found"));

		UserRole userRole = userRoleRepository.findByUserIdAndRole(user.getId(), role)
				.orElseThrow(() -> new InvalidRequestException("User role not found"));

		long roleCount = userRoleRepository.countByUserId(userId);

		// If this is the last role, do not allow removal without replacement
		if (roleCount == 1) {
			// If the only role is already USER, cannot remove (would leave no roles)
			if ("USER".equalsIgnoreCase(roleName)) {
				throw new InvalidRequestException("Cannot remove the last role (USER). User must have at least one role.");
			}
			// Replace the last privileged role with USER
			Role userRoleEntity = roleRepository.findByName("USER")
					.orElseThrow(() -> new InvalidRequestException("Default role USER not found"));
			UserRole newUserRole = UserRole.builder()
					.user(user)
					.role(userRoleEntity)
					.build();
			userRoleRepository.delete(userRole);          // remove the old role
			userRoleRepository.save(newUserRole);         // assign USER
			log.info("Replaced last role {} with USER for user {}", roleName, userId);
			return;
		}

		// More than one role, safe to delete
		userRoleRepository.delete(userRole);
		log.info("Removed role {} from user {}", roleName, userId);
	}

	@Override
	@Transactional
	public void addAuthMethod(UUID userId, UserDto.AuthMethodCreateRequest authMethodCreateRequest) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		authMethodRepository.findByUserIdAndAuthType(user.getId(), authMethodCreateRequest.authType())
				.ifPresent(am -> {
					throw new ConflictException("Authentication method already exists for this user");
				});

		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUser(user);
		authMethod.setAuthType(authMethodCreateRequest.authType());

		// For EMAIL_PASS, hash the password before storing
		if (authMethodCreateRequest.authType() == AuthProvider.EMAIL_PASS) {
			authMethod.setCredential(passwordEncoder.encode(authMethodCreateRequest.credential()));
		} else {
			// For OAuth (GOOGLE, APPLE), store OAuth ID directly
			authMethod.setCredential(authMethodCreateRequest.credential());
		}

		authMethod.setVerified(false);
		authMethod.setCreatedAt(OffsetDateTime.now());

		authMethodRepository.save(authMethod);
	}

	@Override
	@Transactional
	public void updateUserLocation(UUID userId, UserDto.UserLocationRequest request) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		UserLocation location = new UserLocation();
		location.setUser(user);
		location.setLocation(createGeographyPoint(request.latitude(), request.longitude()));
		location.setAccuracyMeters(request.accuracyMeters().floatValue());
		location.setRecordedAt(request.recordedAt() != null ? request.recordedAt() : OffsetDateTime.now());

		userLocationRepository.save(location);
	}

	@Override
	@Transactional
	public UserDto.UserResponse blockUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		user.setStatus(UserStatus.BLOCKED);
		userRepository.save(user);

		// Broadcast to Kafka topic so Gateway updates Redis immediately
		Map<String, Object> params = new HashMap<>();
		params.put("phoneNumber", user.getPhoneNumber());
		params.put("email", user.getEmail());

		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.userId(userId)
				.type(UserStatusEvent.USER_BLOCKED)
				.jti(null)
				.ttlMillis(0)
				.templateKey(NotificationTemplateKey.ADMIN_USER_BLOCKED)
				.parameters(params)
				.build();
		eventProducer.sendUserSecurityEvent(event);

		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public UserDto.UserResponse unblockUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);

		// Broadcast to Kafka to remove from Redis blocklist
		Map<String, Object> params = new HashMap<>();
		params.put("phoneNumber", user.getPhoneNumber());
		params.put("email", user.getEmail());

		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.userId(userId)
				.type(UserStatusEvent.USER_UNBLOCKED)
				.jti(null)
				.ttlMillis(0)
				.templateKey(NotificationTemplateKey.ADMIN_USER_UNBLOCKED)
				.parameters(params)
				.build();
		eventProducer.sendUserSecurityEvent(event);

		return userMapper.toResponse(user);
	}

	// Helper to create PostGIS geography point (depends on your JPA configuration)
	private Point createGeographyPoint(Double latitude, Double longitude) {
		// This depends on your PostGIS integration, e.g., using org.locationtech.jts.geom.Point or Hibernate Spatial
		// Example with JTS:
		// GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
		// return gf.createPoint(new Coordinate(longitude, latitude));
		throw new BusinessException("Implement geography point creation based on your PostGIS setup");
	}
}