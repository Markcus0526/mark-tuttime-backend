package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.entity.enums.VerificationType;
import com.tuttime.user.kafka.UserEventProducer;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.*;
import com.tuttime.user.service.EmailService;
import com.tuttime.user.service.SmsService;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Service
@RequiredArgsConstructor
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
	private final UserEventProducer userEventProducer;
	private final TokenUtil tokenUtil;

	@Override
	@Transactional
	public UserDto.UserResponse createUser(UserDto.UserCreateRequest request) {
		String identifier = request.emailOrPhone();
		String roleName = request.roleName();

		if (!StringUtils.hasText(identifier)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}
		if (!StringUtils.hasText(roleName)) {
			throw new InvalidRequestException("Role must be provided");
		}

		String phoneNumber = null;
		String email = null;
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

		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new InvalidRequestException("Role " + roleName + " not found"));
		UserRole userRole = UserRole.builder()
				.user(user)
				.role(role)
				.build();
		user.getRoles().add(userRole);

		// 2. Create and link AuthMethod to the SAVED user
		String password = tokenUtil.generateRandomPassword();
		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUser(user); // Now 'user' has an ID!
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

		return userMapper.toResponse(user);
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
	public UserDto.UserResponse updateUser(UUID id, UserDto.UserUpdateRequest userUpdateRequest) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		if (userUpdateRequest.phoneNumber() != null && !userUpdateRequest.phoneNumber().equals(user.getPhoneNumber())) {
			userRepository.findByPhoneNumberAndDeletedFalse(userUpdateRequest.phoneNumber())
					.ifPresent(u -> {
						throw new ConflictException("Phone number already in use");
					});
			user.setPhoneNumber(userUpdateRequest.phoneNumber());
		}
		if (userUpdateRequest.email() != null && !userUpdateRequest.email().equals(user.getEmail())) {
			userRepository.findByEmailAndDeletedFalse(userUpdateRequest.email())
					.ifPresent(u -> {
						throw new ConflictException("Email already in use");
					});
			user.setEmail(userUpdateRequest.email());
		}
		if (userUpdateRequest.fullName() != null) {
			user.setFullName(userUpdateRequest.fullName());
		}
		if (userUpdateRequest.profilePicture() != null) {
			user.setProfilePicture(userUpdateRequest.profilePicture());
		}
		user = userRepository.save(user);

		userEventProducer.sendUserUpdated(user);

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
		userEventProducer.sendUserDeleted(userId);

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
		userEventProducer.sendUserBlocked(userId);

		return userMapper.toResponse(user);
	}

	@Transactional
	public UserDto.UserResponse unblockUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);

		// Broadcast to Kafka to remove from Redis blocklist
		userEventProducer.sendUserUnblocked(userId);

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