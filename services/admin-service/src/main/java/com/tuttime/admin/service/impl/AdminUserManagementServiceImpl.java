package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.rest.client.UserServiceRestClient;
import com.tuttime.admin.service.AdminUserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin User Management Service Implementation
 *
 * <p>Provides management features for regular users (not admin users).
 * This service delegates to user-service via REST client.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserManagementServiceImpl implements AdminUserManagementService {

	private final UserServiceRestClient userServiceRestClient;

	@Override
	public Page<UserSummaryResponse> listUsers(String search, String status, String kycStatus, Pageable pageable) {
		Map<String, Object> pageData = userServiceRestClient.listUsers(search, status, kycStatus, false, pageable);
		return convertMapToUserSummaryPage(pageData);
	}

	@Override
	public UserDetailResponse getUserDetails(Long userId) {
		Map<String, Object> userData = userServiceRestClient.getUserById(userId, false);
		return convertMapToUserDetailResponse(userData);
	}

	@Override
	public UserDetailResponse updateUser(Long userId, UpdateUserRequest request) {
		Map<String, Object> userData = userServiceRestClient.updateUser(userId, request);
		return convertMapToUserDetailResponse(userData);
	}

	@Override
	public UserDetailResponse blockUser(Long userId, String reason) {
		Map<String, Object> userData = userServiceRestClient.blockUser(userId, reason);
		return convertMapToUserDetailResponse(userData);
	}

	@Override
	public UserDetailResponse unblockUser(Long userId) {
		Map<String, Object> userData = userServiceRestClient.unblockUser(userId);
		return convertMapToUserDetailResponse(userData);
	}

	@Override
	public UserDetailResponse approveKyc(Long userId) {
		userServiceRestClient.forceKyc(userId, Map.of());
		// Re-fetch user details after approval
		return getUserDetails(userId);
	}

	@Override
	public UserDetailResponse rejectKyc(Long userId, String reason) {
		// Note: This might need to be implemented in UserServiceRestClient if not exists
		// For now, return user details - the actual rejection should be handled by user-service
		log.warn("Reject KYC called but may not be fully implemented in REST client: userId={}, reason={}", userId, reason);
		return getUserDetails(userId);
	}

	@Override
	public void deleteUser(Long userId) {
		userServiceRestClient.deleteUser(userId);
	}

	@Override
	public ReferralNetworkResponse getUserReferrals(Long userId) {
		Map<String, Object> referralData = userServiceRestClient.getUserReferrals(userId);
		return convertMapToReferralNetworkResponse(referralData);
	}

	@Override
	public String createReferral(Long referrerId, Long refereeId, Integer level) {
		// TODO: Implement createReferral in UserServiceRestClient
		log.warn("createReferral not yet implemented in UserServiceRestClient: referrerId={}, refereeId={}, level={}",
				referrerId, refereeId, level);
		throw new UnsupportedOperationException("createReferral not yet implemented");
	}

	@Override
	public BulkOperationResponse bulkBlockUsers(BulkOperationRequest request) {
		// TODO: Implement bulkBlockUsers in UserServiceRestClient
		log.warn("bulkBlockUsers not yet implemented in UserServiceRestClient");
		throw new UnsupportedOperationException("bulkBlockUsers not yet implemented");
	}

	@Override
	public BulkOperationResponse bulkUnblockUsers(BulkOperationRequest request) {
		// TODO: Implement bulkUnblockUsers in UserServiceRestClient
		log.warn("bulkUnblockUsers not yet implemented in UserServiceRestClient");
		throw new UnsupportedOperationException("bulkUnblockUsers not yet implemented");
	}

	@Override
	public BulkOperationResponse bulkApproveKyc(BulkOperationRequest request) {
		// TODO: Implement bulkApproveKyc in UserServiceRestClient
		log.warn("bulkApproveKyc not yet implemented in UserServiceRestClient");
		throw new UnsupportedOperationException("bulkApproveKyc not yet implemented");
	}

	@Override
	public BulkOperationResponse bulkRejectKyc(BulkOperationRequest request) {
		// TODO: Implement bulkRejectKyc in UserServiceRestClient
		log.warn("bulkRejectKyc not yet implemented in UserServiceRestClient");
		throw new UnsupportedOperationException("bulkRejectKyc not yet implemented");
	}

	@Override
	public BulkOperationResponse bulkDeleteUsers(BulkOperationRequest request) {
		// TODO: Implement bulkDeleteUsers in UserServiceRestClient
		log.warn("bulkDeleteUsers not yet implemented in UserServiceRestClient");
		throw new UnsupportedOperationException("bulkDeleteUsers not yet implemented");
	}

	@Override
	public Map<String, Object> getUserOverview(Long userId, Integer conversionPage, Integer conversionSize,
	                                           Integer transactionPage, Integer transactionSize,
	                                           Integer boostPage, Integer boostSize) {
		return userServiceRestClient.getUserOverview(userId, conversionPage, conversionSize,
				transactionPage, transactionSize, boostPage, boostSize);
	}

	@Override
	public Map<String, Object> getExtendedUserDetail(Long userId) {
		return userServiceRestClient.getExtendedUserDetail(userId);
	}

	@Override
	public void resetMining(Long userId, Map<String, Object> request) {
		userServiceRestClient.resetMining(userId, request);
	}

	@Override
	public void pauseAccount(Long userId, Map<String, Object> request) {
		userServiceRestClient.pauseAccount(userId, request);
	}

	@Override
	public void unpauseAccount(Long userId) {
		userServiceRestClient.unpauseAccount(userId);
	}

	@Override
	public Map<String, Object> addBonus(Long userId, Map<String, Object> request) {
		return userServiceRestClient.addBonus(userId, request);
	}

	@Override
	public void freezeWithdrawal(Long userId, Map<String, Object> request) {
		userServiceRestClient.freezeWithdrawal(userId, request);
	}

	@Override
	public void unfreezeWithdrawal(Long userId) {
		userServiceRestClient.unfreezeWithdrawal(userId);
	}

	@Override
	public void markAntiFraud(Long userId, Map<String, Object> request) {
		userServiceRestClient.markAntiFraud(userId, request);
	}

	@Override
	public void unmarkAntiFraud(Long userId) {
		userServiceRestClient.unmarkAntiFraud(userId);
	}

	@Override
	public Map<String, Object> deleteBetaTesterByPhone(String phoneNumber) {
		// TODO: Implement deleteBetaTesterByPhone in UserServiceRestClient
		log.warn("deleteBetaTesterByPhone not yet implemented in UserServiceRestClient: phoneNumber={}", phoneNumber);
		throw new UnsupportedOperationException("deleteBetaTesterByPhone not yet implemented");
	}

	// Helper methods to convert Map to DTOs
	private Page<UserSummaryResponse> convertMapToUserSummaryPage(Map<String, Object> pageData) {
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.getOrDefault("content", List.of());

		List<UserSummaryResponse> users = content.stream()
				.map(this::convertMapToUserSummary)
				.collect(Collectors.toList());

		long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
		int number = ((Number) pageData.getOrDefault("number", 0)).intValue();
		int size = ((Number) pageData.getOrDefault("size", 20)).intValue();

		return new PageImpl<>(users, PageRequest.of(number, size), totalElements);
	}

	private UserSummaryResponse convertMapToUserSummary(Map<String, Object> userMap) {
		// Map status based on isActive and isBlocked
		String status = determineStatus(userMap);

		// Map KYC status
		String kycStatus = determineKycStatus(userMap);

		// Convert BigDecimal balances to Long (AP Credits and APART tokens are stored as Long in admin DTO)
		Long apBalance = convertBigDecimalToLong(userMap.get("currentApBalance"), userMap.get("totalApBalance"));
		Long apartBalance = convertBigDecimalToLong(userMap.get("totalApartBalance"));

		return UserSummaryResponse.builder()
				.userId(getLong(userMap, "userId"))
				.email((String) userMap.get("email"))
				.username((String) userMap.get("username"))
				.firstName((String) userMap.get("firstName"))
				.lastName((String) userMap.get("lastName"))
				.phone((String) userMap.get("phone"))
				.telegram((String) userMap.get("telegram"))
				.profileImageUrl((String) userMap.get("profileImageUrl"))
				.country((String) userMap.get("country"))
				.city((String) userMap.get("city"))
				.latitude(getBigDecimal(userMap, "latitude"))
				.longitude(getBigDecimal(userMap, "longitude"))
				.locationAccuracy(getInteger(userMap, "locationAccuracy"))
				.locationUpdatedAt(parseOffsetDateTime(userMap.get("locationUpdatedAt")))
				.locationPrivacyLevel((String) userMap.get("locationPrivacyLevel"))
				.locationSharingEnabled(getBoolean(userMap, "locationSharingEnabled"))
				.isVerified(getBoolean(userMap, "isVerified"))
				.emailVerified(getBoolean(userMap, "emailVerified"))
				.kycCompleted(getBoolean(userMap, "kycCompleted"))
				.kycVerifiedAt(parseOffsetDateTime(userMap.get("kycVerifiedAt")))
				.status(status)
				.kycStatus(kycStatus)
				.isActive(getBoolean(userMap, "isActive"))
				.isBlocked(getBoolean(userMap, "isBlocked"))
				.blockedReason((String) userMap.get("blockedReason"))
				.blockedAt(parseOffsetDateTime(userMap.get("blockedAt")))
				.isPaused(getBoolean(userMap, "isPaused")) // May not be in UserResponse, will be null if not present
				.withdrawalFrozen(getBoolean(userMap, "withdrawalFrozen")) // May not be in UserResponse
				.antiFraudWatch(getBoolean(userMap, "antiFraudWatch")) // May not be in UserResponse
				.registrationDate(parseOffsetDateTime(userMap.get("registrationDate")))
				.lastActive(parseOffsetDateTime(userMap.get("lastActive")))
				.lastLogin(parseOffsetDateTime(userMap.get("lastLogin")))
				.createdAt(parseOffsetDateTime(userMap.get("createdAt")))
				.updatedAt(parseOffsetDateTime(userMap.get("updatedAt")))
				.apBalance(apBalance)
				.apartBalance(apartBalance)
				.build();
	}

	private UserDetailResponse convertMapToUserDetailResponse(Map<String, Object> userMap) {
		java.time.OffsetDateTime lastLogin = parseOffsetDateTime(userMap.get("lastLogin"));

		// Map status based on isActive and isBlocked
		String status = determineStatus(userMap);

		// Map KYC status
		String kycStatus = determineKycStatus(userMap);

		// Convert BigDecimal balances to Long
		Long apBalance = convertBigDecimalToLong(userMap.get("currentApBalance"), userMap.get("totalApBalance"));
		Long apartBalance = convertBigDecimalToLong(userMap.get("totalApartBalance"));

		// Get referral count from totalFriendsCount if available
		Integer referralCount = getInteger(userMap, "referralCount");
		if (referralCount == null) {
			referralCount = getInteger(userMap, "totalFriendsCount");
		}

		return UserDetailResponse.builder()
				.userId(getLong(userMap, "userId"))
				.email((String) userMap.get("email"))
				.username((String) userMap.get("username"))
				.firstName((String) userMap.get("firstName"))
				.lastName((String) userMap.get("lastName"))
				.phone((String) userMap.get("phone"))
				.telegram((String) userMap.get("telegram"))
				.profileImageUrl((String) userMap.get("profileImageUrl"))
				.country((String) userMap.get("country"))
				.city((String) userMap.get("city"))
				.dateOfBirth((String) userMap.get("dateOfBirth")) // May not be in UserResponse
				.latitude(getBigDecimal(userMap, "latitude"))
				.longitude(getBigDecimal(userMap, "longitude"))
				.locationAccuracy(getInteger(userMap, "locationAccuracy"))
				.locationUpdatedAt(parseOffsetDateTime(userMap.get("locationUpdatedAt")))
				.locationPrivacyLevel((String) userMap.get("locationPrivacyLevel"))
				.locationSharingEnabled(getBoolean(userMap, "locationSharingEnabled"))
				.isVerified(getBoolean(userMap, "isVerified"))
				.emailVerified(getBoolean(userMap, "emailVerified"))
				.kycCompleted(getBoolean(userMap, "kycCompleted"))
				.kycVerifiedAt(parseOffsetDateTime(userMap.get("kycVerifiedAt")))
				.status(status)
				.kycStatus(kycStatus)
				.isActive(getBoolean(userMap, "isActive"))
				.isBlocked(getBoolean(userMap, "isBlocked"))
				.blockedReason((String) userMap.get("blockedReason"))
				.blockedAt(parseOffsetDateTime(userMap.get("blockedAt")))
				.isPaused(getBoolean(userMap, "isPaused")) // May not be in UserResponse
				.withdrawalFrozen(getBoolean(userMap, "withdrawalFrozen")) // May not be in UserResponse
				.antiFraudWatch(getBoolean(userMap, "antiFraudWatch")) // May not be in UserResponse
				.registrationDate(parseOffsetDateTime(userMap.get("registrationDate")))
				.lastActive(parseOffsetDateTime(userMap.get("lastActive")))
				.lastLogin(lastLogin)
				.lastLoginAt(lastLogin) // Alias for lastLogin
				.createdAt(parseOffsetDateTime(userMap.get("createdAt")))
				.updatedAt(parseOffsetDateTime(userMap.get("updatedAt")))
				.apBalance(apBalance)
				.apartBalance(apartBalance)
				.deviceId((String) userMap.get("deviceId")) // May not be in UserResponse
				.lastIpAddress((String) userMap.get("lastIpAddress")) // May not be in UserResponse
				.referralCount(referralCount)
				.userStatusLevel((String) userMap.getOrDefault("userStatusLevel", userMap.get("statusLevel"))) // Map from statusLevel if userStatusLevel not present
				.build();
	}

	private ReferralNetworkResponse convertMapToReferralNetworkResponse(Map<String, Object> referralMap) {
		// This is a simplified conversion - you may need to adjust based on actual ReferralNetworkResponse fields
		return ReferralNetworkResponse.builder()
				.build();
	}

	// Helper methods for type conversion
	private Long getLong(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) return null;
		if (value instanceof Number) {
			return ((Number) value).longValue();
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			log.warn("Failed to parse Long for key {}: {}", key, value);
			return null;
		}
	}

	private Integer getInteger(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) return null;
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		try {
			return Integer.parseInt(value.toString());
		} catch (NumberFormatException e) {
			log.warn("Failed to parse Integer for key {}: {}", key, value);
			return null;
		}
	}

	private Boolean getBoolean(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) return null;
		if (value instanceof Boolean) {
			return (Boolean) value;
		}
		if (value instanceof String) {
			return Boolean.parseBoolean((String) value);
		}
		return null;
	}

	private java.math.BigDecimal getBigDecimal(Map<String, Object> data, String key) {
		Object value = data.get(key);
		if (value == null) return null;
		if (value instanceof java.math.BigDecimal) {
			return (java.math.BigDecimal) value;
		}
		if (value instanceof Number) {
			return java.math.BigDecimal.valueOf(((Number) value).doubleValue());
		}
		try {
			return new java.math.BigDecimal(value.toString());
		} catch (NumberFormatException e) {
			log.warn("Failed to parse BigDecimal for key {}: {}", key, value);
			return null;
		}
	}

	private java.time.OffsetDateTime parseOffsetDateTime(Object value) {
		if (value == null) return null;
		if (value instanceof String) {
			try {
				return java.time.OffsetDateTime.parse((String) value);
			} catch (Exception e) {
				log.warn("Failed to parse OffsetDateTime: {}", value, e);
				return null;
			}
		}
		return null;
	}

	/**
	 * Determine user status based on isActive and isBlocked flags
	 */
	private String determineStatus(Map<String, Object> userMap) {
		Boolean isBlocked = getBoolean(userMap, "isBlocked");
		Boolean isActive = getBoolean(userMap, "isActive");
		Boolean isPaused = getBoolean(userMap, "isPaused");

		// Check if status is already provided
		String status = (String) userMap.get("status");
		if (status != null && !status.isEmpty()) {
			return status;
		}

		// Determine status from flags
		if (Boolean.TRUE.equals(isBlocked)) {
			return "BLOCKED";
		}
		if (Boolean.TRUE.equals(isPaused)) {
			return "PAUSED";
		}
		if (Boolean.FALSE.equals(isActive)) {
			return "INACTIVE";
		}
		return "ACTIVE";
	}

	/**
	 * Determine KYC status based on kycCompleted flag
	 */
	private String determineKycStatus(Map<String, Object> userMap) {
		// Check if kycStatus is already provided
		String kycStatus = (String) userMap.get("kycStatus");
		if (kycStatus != null && !kycStatus.isEmpty()) {
			return kycStatus;
		}

		// Determine from kycCompleted flag
		Boolean kycCompleted = getBoolean(userMap, "kycCompleted");
		if (Boolean.TRUE.equals(kycCompleted)) {
			return "APPROVED";
		}
		return "PENDING";
	}

	/**
	 * Convert BigDecimal to Long (for AP Credits and APART tokens)
	 * Tries multiple keys in order of preference
	 */
	private Long convertBigDecimalToLong(Object... values) {
		for (Object value : values) {
			if (value == null) continue;

			if (value instanceof java.math.BigDecimal) {
				return ((java.math.BigDecimal) value).longValue();
			}
			if (value instanceof Number) {
				return ((Number) value).longValue();
			}
			if (value instanceof String) {
				try {
					return Long.parseLong((String) value);
				} catch (NumberFormatException e) {
					try {
						return java.math.BigDecimal.valueOf(Double.parseDouble((String) value)).longValue();
					} catch (NumberFormatException ex) {
						// Ignore
					}
				}
			}
		}
		return null;
	}
}

