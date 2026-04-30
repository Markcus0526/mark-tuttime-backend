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
import com.tuttime.user.repository.*;
import com.tuttime.user.service.UserService;
import com.tuttime.user.util.TokenUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
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

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Service
@RequiredArgsConstructor
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
	private final TokenUtil tokenUtil;

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
		user = userRepository.saveAndFlush(user);

		// 4. Assign Role
		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new InvalidRequestException("Role " + roleName + " not found"));
		user.getRoles().add(UserRole.builder().user(user).role(role).build());

		// 5. Setup Password & Auth Method
		String rawPassword = tokenUtil.generateRandomPassword();
		UserAuthMethod authMethod = UserAuthMethod.builder()
				.user(user)
				.credential(passwordEncoder.encode(rawPassword))
				.authType(email != null ? AuthProvider.EMAIL_PASS : AuthProvider.PHONE_SMS)
				.verified(false)
				.build();
		user.addAuthMethod(authMethod);
		authMethodRepository.save(authMethod);

		// 6. Generate Verification Code
		String code = tokenUtil.generateRandomCode();
		VerificationType type = (phoneNumber != null) ? VerificationType.PHONE_VERIFY : VerificationType.EMAIL_VERIFY;

		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(type);
		verificationCode.setTarget(identifier);
		verificationCode.setCodeHash(passwordEncoder.encode(code));
		verificationCode.setExpiresAt(OffsetDateTime.now().plusHours(1)); // Extended for manual creation
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		// 7. Fire Kafka Event to Notification Service
		// We include the 'password' in params so the user knows how to log in
		Map<String, Object> params = new HashMap<>();
		params.put("verificationCode", code);
		params.put("temporaryPassword", rawPassword);

		UserEvent.VerificationEvent event = UserEvent.VerificationEvent.builder()
				.userId(user.getId())
				.email(email)
				.phoneNumber(phoneNumber)
				.type(type)
				.templateKey(NotificationTemplateKey.ADMIN_USER_CREATED) // Specific template for admin-created users
				.parameters(params)
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

		if (request.profilePicture() != null) {
			user.setProfilePicture(request.profilePicture());
		}

		user = userRepository.save(user);

		UserEvent.ProfileUpdatedEvent event = UserEvent.ProfileUpdatedEvent.builder()
				.userId(user.getId())
				.fullName(user.getFullName())
				.profilePicture(user.getProfilePicture())
				.updatedAt(user.getUpdatedAt())
				.build();
		eventProducer.sendUserUpdatedEvent(event);

		return userMapper.toResponse(user);
	}

	@Override
	@Transactional
	public UserDto.UserResponse updateMe(UUID id, UserDto.UserUpdateRequest request, MultipartFile file) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		if (file != null && !file.isEmpty()) {
			// Clean up old file to save space/money
			if (StringUtils.hasText(user.getProfilePicture())) {
				storageProvider.delete(user.getProfilePicture());
			}

			// Store new file and get the key
			String key = storageProvider.store(file);
			user.setProfilePicture(key);
		}

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

		if (request.profilePicture() != null) {
			user.setProfilePicture(request.profilePicture());
		}

		user = userRepository.save(user);

		UserEvent.ProfileUpdatedEvent event = UserEvent.ProfileUpdatedEvent.builder()
				.userId(user.getId())
				.fullName(user.getFullName())
				.profilePicture(user.getProfilePicture())
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
		// This tells the Gateway and Redis to block this ID permanently
		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.userId(userId)
				.type(UserStatusEvent.USER_DELETED)
				.jti(null)
				.ttlMillis(0)
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

		userRoleRepository.delete(userRole);
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

	@Transactional
	public UserDto.UserResponse blockUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		user.setStatus(UserStatus.BLOCKED);
		userRepository.save(user);

		// Broadcast to Kafka topic so Gateway updates Redis immediately
		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.userId(userId)
				.type(UserStatusEvent.USER_BLOCKED)
				.jti(null)
				.ttlMillis(0)
				.build();
		eventProducer.sendUserSecurityEvent(event);

		return userMapper.toResponse(user);
	}

	@Transactional
	public UserDto.UserResponse unblockUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);

		// Broadcast to Kafka to remove from Redis blocklist
		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.userId(userId)
				.type(UserStatusEvent.USER_UNBLOCKED)
				.jti(null)
				.ttlMillis(0)
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