package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.entity.FraudFlag;
import com.tuttime.admin.repository.FraudFlagRepository;
import com.tuttime.admin.rest.client.UserServiceRestClient;
import com.tuttime.admin.service.AdminAntiFraudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Admin Anti-Fraud Service Implementation
 *
 * <p>Manages fraud flags, device blocking, IP blocking, and whitelisting using Redis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAntiFraudServiceImpl implements AdminAntiFraudService {

	@Autowired(required = false)
	private RedisTemplate<String, String> redisTemplate;

	private final FraudFlagRepository fraudFlagRepository;
	private final UserServiceRestClient userServiceRestClient;

	private static final String BLOCKED_DEVICE_PREFIX = "admin:blocked:device:";
	private static final String BLOCKED_IP_PREFIX = "admin:blocked:ip:";
	private static final String WHITELISTED_DEVICE_PREFIX = "admin:whitelisted:device:";
	private static final String WHITELISTED_IP_PREFIX = "admin:whitelisted:ip:";

	@Override
	public Page<FraudFlagResponse> listFraudFlags(Pageable pageable,
	                                              Long userId,
	                                              WarningCategory category,
	                                              WarningSeverity severity,
	                                              Boolean resolved) {
		log.debug("Listing fraud flags with filters - userId: {}, category: {}, severity: {}, resolved: {}",
				userId, category, severity, resolved);

		try {
			// Build specification with optional filters
			Specification<FraudFlag> spec = Specification.where(null);

			if (userId != null) {
				spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
			}
			if (category != null) {
				spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
			}
			if (severity != null) {
				spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
			}
			if (resolved != null) {
				spec = spec.and((root, query, cb) -> cb.equal(root.get("resolved"), resolved));
			}

			// Create pageable with sorting by createdAt descending
			Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
			Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

			Page<FraudFlag> fraudFlags = fraudFlagRepository.findAll(spec, sortedPageable);

			return fraudFlags.map(this::mapToFraudFlagResponse);
		} catch (Exception e) {
			log.error("Error listing fraud flags", e);
			return new PageImpl<>(new ArrayList<>());
		}
	}

	/**
	 * Map FraudFlag entity to FraudFlagResponse DTO
	 */
	private FraudFlagResponse mapToFraudFlagResponse(FraudFlag fraudFlag) {
		return FraudFlagResponse.builder()
				.flagId(fraudFlag.getFlagId())
				.userId(fraudFlag.getUserId())
				.flagType(fraudFlag.getFlagType())
				.severity(fraudFlag.getSeverity())
				.category(fraudFlag.getCategory())
				.description(fraudFlag.getDescription())
				.deviceId(fraudFlag.getDeviceId())
				.ipAddress(fraudFlag.getIpAddress())
				.createdAt(fraudFlag.getCreatedAt())
				.resolved(fraudFlag.getResolved())
				.build();
	}

	@Override
	public void blockDevice(String deviceId, String reason) {
		if (redisTemplate == null) {
			log.warn("Redis not available - device blocking skipped");
			return;
		}
		try {
			// Query user-service to get device information
			Map<String, Object> deviceInfo = null;
			Long userId = null;
			try {
				deviceInfo = userServiceRestClient.getDeviceByClientDeviceId(deviceId);
				if (deviceInfo != null && deviceInfo.get("userId") != null) {
					userId = Long.valueOf(deviceInfo.get("userId").toString());
					log.info("Found device in user-service: deviceId={}, userId={}", deviceId, userId);

					// Update device status in database: set isActive=false, isTrusted=false
					try {
						Map<String, Object> updateResult = userServiceRestClient.updateDeviceStatus(deviceId, false, false);
						if (updateResult != null) {
							log.info("Updated device status in database: deviceId={}, isActive=false, isTrusted=false, result={}",
									deviceId, updateResult);
						} else {
							log.warn("Device status update returned null - deviceId={}", deviceId);
						}
					} catch (Exception e) {
						String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
						log.error("Failed to update device status in database: deviceId={}, error={}, stackTrace={}", deviceId, e.getMessage(), stackTraceSummary);
						// Continue with blocking even if database update fails
					}
				}
			} catch (Exception e) {
				log.warn("Failed to query user-service for device info: deviceId={}, error={}", deviceId, e.getMessage());
				// Continue with blocking even if user-service query fails
			}

			String key = BLOCKED_DEVICE_PREFIX + deviceId;
			// Block for 1 year (effectively permanent until manually unblocked)
			redisTemplate.opsForValue().set(key, reason != null ? reason : "Admin blocked",
					365, TimeUnit.DAYS);
			log.info("Device blocked: deviceId={}, userId={}, reason={}", deviceId, userId, reason);

			// Optionally create a fraud flag if device is associated with a user
			if (userId != null) {
				try {
					createFraudFlagForDeviceBlock(userId, deviceId, reason, deviceInfo);
				} catch (Exception e) {
					log.warn("Failed to create fraud flag for device block: deviceId={}, userId={}", deviceId, userId, e);
					// Don't fail the block operation if fraud flag creation fails
				}
			}
		} catch (Exception e) {
			log.error("Error blocking device: deviceId={}", deviceId, e);
			throw new RuntimeException("Failed to block device: " + e.getMessage(), e);
		}
	}

	/**
	 * Create a fraud flag when a device is blocked
	 */
	private void createFraudFlagForDeviceBlock(Long userId, String deviceId, String reason, Map<String, Object> deviceInfo) {
		try {
			String ipAddress = deviceInfo != null && deviceInfo.get("ipAddress") != null
					? deviceInfo.get("ipAddress").toString() : null;

			FraudFlag fraudFlag = FraudFlag.builder()
					.userId(userId)
					.flagType("DEVICE_BLOCKED")
					.severity(com.tuttime.admin.dto.WarningSeverity.HIGH)
					.category(com.tuttime.admin.dto.WarningCategory.MALICIOUS)
					.description("Device blocked by admin: " + (reason != null ? reason : "No reason provided"))
					.deviceId(deviceId)
					.ipAddress(ipAddress)
					.resolved(false)
					.build();

			fraudFlagRepository.save(fraudFlag);
			log.info("Created fraud flag for device block: deviceId={}, userId={}, flagId={}",
					deviceId, userId, fraudFlag.getFlagId());
		} catch (Exception e) {
			log.error("Error creating fraud flag for device block: deviceId={}, userId={}", deviceId, userId, e);
			throw e;
		}
	}

	@Override
	public void unblockDevice(String deviceId) {
		if (redisTemplate == null) {
			return;
		}
		try {
			// Update device status in database: set isActive=true and isTrusted=true
			// When unblocking, we restore both active and trusted status
			try {
				userServiceRestClient.updateDeviceStatus(deviceId, true, true);
				log.info("Updated device status in database: deviceId={}, isActive=true, isTrusted=true", deviceId);
			} catch (Exception e) {
				log.warn("Failed to update device status in database: deviceId={}, error={}", deviceId, e.getMessage());
				// Continue with unblocking even if database update fails
			}

			String key = BLOCKED_DEVICE_PREFIX + deviceId;
			redisTemplate.delete(key);
			log.info("Device unblocked: deviceId={}", deviceId);
		} catch (Exception e) {
			log.error("Error unblocking device: deviceId={}", deviceId, e);
			throw new RuntimeException("Failed to unblock device: " + e.getMessage(), e);
		}
	}

	@Override
	public void blockIpAddress(String ipAddress, String reason) {
		if (redisTemplate == null) {
			log.warn("Redis not available - IP blocking skipped");
			return;
		}
		try {
			// Block IP in Redis
			String key = BLOCKED_IP_PREFIX + ipAddress;
			// Block for 1 year (effectively permanent until manually unblocked)
			redisTemplate.opsForValue().set(key, reason != null ? reason : "Admin blocked",
					365, TimeUnit.DAYS);
			log.info("IP address blocked in Redis: ip={}, reason={}", ipAddress, reason);

			// Block all devices with this IP address in user-service
			try {
				java.util.List<Map<String, Object>> devices = userServiceRestClient.getDevicesByIpAddress(ipAddress);
				if (devices != null && !devices.isEmpty()) {
					log.info("Found {} devices with IP address {} - blocking all devices", devices.size(), ipAddress);
					int blockedCount = 0;
					for (Map<String, Object> device : devices) {
						try {
							String clientDeviceId = device.get("clientDeviceId") != null
									? device.get("clientDeviceId").toString()
									: null;

							// If no clientDeviceId, try using deviceId as fallback
							if (clientDeviceId == null || clientDeviceId.isEmpty()) {
								Object deviceIdObj = device.get("deviceId");
								if (deviceIdObj != null) {
									clientDeviceId = deviceIdObj.toString();
								}
							}

							if (clientDeviceId != null && !clientDeviceId.isEmpty()) {
								// Block device: set isActive=false, isTrusted=false
								Map<String, Object> updateResult = userServiceRestClient.updateDeviceStatus(
										clientDeviceId, false, false);
								if (updateResult != null) {
									blockedCount++;
									log.info("Blocked device with IP {}: clientDeviceId={}, deviceId={}, userId={}",
											ipAddress, clientDeviceId, device.get("deviceId"), device.get("userId"));
								}
							} else {
								log.warn("Skipping device with IP {} - no clientDeviceId or deviceId found: {}",
										ipAddress, device);
							}
						} catch (Exception e) {
							log.error("Error blocking device with IP {}: device={}, error={}",
									ipAddress, device, e.getMessage(), e);
							// Continue blocking other devices even if one fails
						}
					}
					log.info("Blocked {} out of {} devices with IP address {}", blockedCount, devices.size(), ipAddress);
				} else {
					log.info("No devices found with IP address {}", ipAddress);
				}
			} catch (Exception e) {
				log.error("Error querying user-service for devices with IP {}: error={}", ipAddress, e.getMessage(), e);
				// Continue with IP blocking even if device blocking fails
			}
		} catch (Exception e) {
			log.error("Error blocking IP address: ip={}", ipAddress, e);
			throw new RuntimeException("Failed to block IP address: " + e.getMessage(), e);
		}
	}

	@Override
	public void unblockIpAddress(String ipAddress) {
		if (redisTemplate == null) {
			return;
		}
		try {
			// Unblock IP in Redis
			String key = BLOCKED_IP_PREFIX + ipAddress;
			redisTemplate.delete(key);
			log.info("IP address unblocked in Redis: ip={}", ipAddress);

			// Unblock all devices with this IP address in user-service
			try {
				java.util.List<Map<String, Object>> devices = userServiceRestClient.getDevicesByIpAddress(ipAddress);
				if (devices != null && !devices.isEmpty()) {
					log.info("Found {} devices with IP address {} - unblocking all devices", devices.size(), ipAddress);
					int unblockedCount = 0;
					for (Map<String, Object> device : devices) {
						try {
							String clientDeviceId = device.get("clientDeviceId") != null
									? device.get("clientDeviceId").toString()
									: null;

							// If no clientDeviceId, try using deviceId as fallback
							if (clientDeviceId == null || clientDeviceId.isEmpty()) {
								Object deviceIdObj = device.get("deviceId");
								if (deviceIdObj != null) {
									clientDeviceId = deviceIdObj.toString();
								}
							}

							if (clientDeviceId != null && !clientDeviceId.isEmpty()) {
								// Unblock device: set isActive=true, isTrusted=true
								Map<String, Object> updateResult = userServiceRestClient.updateDeviceStatus(
										clientDeviceId, true, true);
								if (updateResult != null) {
									unblockedCount++;
									log.info("Unblocked device with IP {}: clientDeviceId={}, deviceId={}, userId={}",
											ipAddress, clientDeviceId, device.get("deviceId"), device.get("userId"));
								}
							} else {
								log.warn("Skipping device with IP {} - no clientDeviceId or deviceId found: {}",
										ipAddress, device);
							}
						} catch (Exception e) {
							log.error("Error unblocking device with IP {}: device={}, error={}",
									ipAddress, device, e.getMessage(), e);
							// Continue unblocking other devices even if one fails
						}
					}
					log.info("Unblocked {} out of {} devices with IP address {}", unblockedCount, devices.size(), ipAddress);
				} else {
					log.info("No devices found with IP address {}", ipAddress);
				}
			} catch (Exception e) {
				log.error("Error querying user-service for devices with IP {}: error={}", ipAddress, e.getMessage(), e);
				// Continue with IP unblocking even if device unblocking fails
			}
		} catch (Exception e) {
			log.error("Error unblocking IP address: ip={}", ipAddress, e);
			throw new RuntimeException("Failed to unblock IP address: " + e.getMessage(), e);
		}
	}

	@Override
	public FraudAnalyticsResponse getFraudAnalytics() {
		// Count blocked devices and IPs from Redis
		long blockedDevicesCount = 0L;
		long blockedIpsCount = 0L;
		long whitelistedDevicesCount = 0L;
		long whitelistedIpsCount = 0L;

		if (redisTemplate != null) {
			try {
				Set<String> blockedDeviceKeys = redisTemplate.keys(BLOCKED_DEVICE_PREFIX + "*");
				blockedDevicesCount = blockedDeviceKeys != null ? blockedDeviceKeys.size() : 0L;

				Set<String> blockedIpKeys = redisTemplate.keys(BLOCKED_IP_PREFIX + "*");
				blockedIpsCount = blockedIpKeys != null ? blockedIpKeys.size() : 0L;

				Set<String> whitelistedDeviceKeys = redisTemplate.keys(WHITELISTED_DEVICE_PREFIX + "*");
				whitelistedDevicesCount = whitelistedDeviceKeys != null ? whitelistedDeviceKeys.size() : 0L;

				Set<String> whitelistedIpKeys = redisTemplate.keys(WHITELISTED_IP_PREFIX + "*");
				whitelistedIpsCount = whitelistedIpKeys != null ? whitelistedIpKeys.size() : 0L;
			} catch (Exception e) {
				log.error("Error counting blocked/whitelisted devices and IPs", e);
			}
		}

		// Count fraud flags from database
		long totalFlags = 0L;
		long unresolvedFlags = 0L;
		List<FraudAnalyticsResponse.FraudTypeStats> fraudTypeStats = new ArrayList<>();

		try {
			totalFlags = fraudFlagRepository.countTotalFlags();
			unresolvedFlags = fraudFlagRepository.countUnresolvedFlags();

			// Aggregate fraud type statistics
			for (WarningCategory cat : WarningCategory.values()) {
				long count = fraudFlagRepository.countByCategory(cat);
				if (count > 0) {
					fraudTypeStats.add(FraudAnalyticsResponse.FraudTypeStats.builder()
							.fraudType(cat.name())
							.count(count)
							.build());
				}
			}
		} catch (Exception e) {
			String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
			log.error("Error counting fraud flags from database, stackTrace={}", stackTraceSummary);
		}

		return FraudAnalyticsResponse.builder()
				.totalFlags(totalFlags)
				.unresolvedFlags(unresolvedFlags)
				.blockedDevices(blockedDevicesCount)
				.blockedIps(blockedIpsCount)
				.whitelistedDevices(whitelistedDevicesCount)
				.whitelistedIps(whitelistedIpsCount)
				.fraudTypeStats(fraudTypeStats)
				.build();
	}

	@Override
	public void whitelistDevice(String deviceId, String reason) {
		if (redisTemplate == null) {
			log.warn("Redis not available - device whitelisting skipped");
			return;
		}
		try {
			String key = WHITELISTED_DEVICE_PREFIX + deviceId;
			// Whitelist for 1 year (effectively permanent until manually removed)
			String value = reason != null ? reason : "Admin whitelisted";
			redisTemplate.opsForValue().set(key, value, 365, TimeUnit.DAYS);
			log.info("Device whitelisted: deviceId={}, reason={}", deviceId, reason);
		} catch (Exception e) {
			log.error("Error whitelisting device: deviceId={}", deviceId, e);
			throw new RuntimeException("Failed to whitelist device: " + e.getMessage(), e);
		}
	}

	@Override
	public void removeDeviceFromWhitelist(String deviceId) {
		if (redisTemplate == null) {
			return;
		}
		try {
			String key = WHITELISTED_DEVICE_PREFIX + deviceId;
			redisTemplate.delete(key);
			log.info("Device removed from whitelist: deviceId={}", deviceId);
		} catch (Exception e) {
			log.error("Error removing device from whitelist: deviceId={}", deviceId, e);
			throw new RuntimeException("Failed to remove device from whitelist: " + e.getMessage(), e);
		}
	}

	@Override
	public void whitelistIpAddress(String ipAddress, String reason) {
		if (redisTemplate == null) {
			log.warn("Redis not available - IP whitelisting skipped");
			return;
		}
		try {
			String key = WHITELISTED_IP_PREFIX + ipAddress;
			// Whitelist for 1 year (effectively permanent until manually removed)
			String value = reason != null ? reason : "Admin whitelisted";
			redisTemplate.opsForValue().set(key, value, 365, TimeUnit.DAYS);
			log.info("IP address whitelisted: ip={}, reason={}", ipAddress, reason);
		} catch (Exception e) {
			log.error("Error whitelisting IP address: ip={}", ipAddress, e);
			throw new RuntimeException("Failed to whitelist IP address: " + e.getMessage(), e);
		}
	}

	@Override
	public void removeIpFromWhitelist(String ipAddress) {
		if (redisTemplate == null) {
			return;
		}
		try {
			String key = WHITELISTED_IP_PREFIX + ipAddress;
			redisTemplate.delete(key);
			log.info("IP address removed from whitelist: ip={}", ipAddress);
		} catch (Exception e) {
			log.error("Error removing IP from whitelist: ip={}", ipAddress, e);
			throw new RuntimeException("Failed to remove IP from whitelist: " + e.getMessage(), e);
		}
	}

	@Override
	public Page<BlockedDeviceResponse> listBlockedDevices(org.springframework.data.domain.Pageable pageable) {
		try {
			// Get blocked devices from Redis
			List<BlockedDeviceResponse> redisDevices = new ArrayList<>();
			if (redisTemplate != null) {
				try {
					Set<String> keys = redisTemplate.keys(BLOCKED_DEVICE_PREFIX + "*");
					if (keys != null && !keys.isEmpty()) {
						redisDevices = keys.stream()
								.map(key -> {
									String deviceId = key.substring(BLOCKED_DEVICE_PREFIX.length());
									String reason = redisTemplate.opsForValue().get(key);
									Long ttl = redisTemplate.getExpire(key);

									OffsetDateTime expiresAt = null;
									Boolean isPermanent = true;
									if (ttl != null && ttl > 0) {
										expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(ttl);
										isPermanent = false;
									}

									return BlockedDeviceResponse.builder()
											.deviceId(deviceId)
											.reason(reason)
											.blockedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(ttl != null && ttl > 0 ?
													(365L * 24 * 60 * 60 - ttl) : 0))
											.expiresAt(expiresAt)
											.isPermanent(isPermanent)
											.build();
								})
								.collect(Collectors.toList());
					}
				} catch (Exception e) {
					log.warn("Error getting blocked devices from Redis: {}", e.getMessage());
				}
			}

			// Get blocked devices from user-service (isActive=false)
			List<BlockedDeviceResponse> dbDevices = new ArrayList<>();
			try {
				Map<String, Object> dbPageData = userServiceRestClient.getBlockedDevices(pageable);
				if (dbPageData != null && dbPageData.get("content") instanceof java.util.List) {
					@SuppressWarnings("unchecked")
					java.util.List<Map<String, Object>> dbDeviceList = (java.util.List<Map<String, Object>>) dbPageData.get("content");

					dbDevices = dbDeviceList.stream()
							.map(deviceMap -> {
								String deviceId = null;
								// Try to get clientDeviceId first, fallback to deviceId
								if (deviceMap.get("clientDeviceId") != null &&
										!deviceMap.get("clientDeviceId").toString().isEmpty()) {
									deviceId = deviceMap.get("clientDeviceId").toString();
								} else if (deviceMap.get("deviceId") != null) {
									deviceId = deviceMap.get("deviceId").toString();
								}

								if (deviceId == null) {
									return null;
								}

								// Get updatedAt as blockedAt if available
								OffsetDateTime blockedAt = null;
								if (deviceMap.get("updatedAt") != null) {
									try {
										blockedAt = OffsetDateTime.parse(deviceMap.get("updatedAt").toString()).withOffsetSameInstant(ZoneOffset.UTC);
									} catch (Exception e) {
										// Ignore parsing errors
									}
								}
								if (blockedAt == null) {
									blockedAt = OffsetDateTime.now(ZoneOffset.UTC);
								}

								return BlockedDeviceResponse.builder()
										.deviceId(deviceId)
										.reason("Blocked via device management")
										.blockedAt(blockedAt)
										.expiresAt(null) // Database blocks are permanent until unblocked
										.isPermanent(true)
										.build();
							})
							.filter(device -> device != null)
							.collect(Collectors.toList());
				}
			} catch (Exception e) {
				log.warn("Error getting blocked devices from user-service: {}", e.getMessage());
			}

			// For each Redis device, check if it's active in the database
			// If it's active, exclude it (it was unblocked)
			List<BlockedDeviceResponse> validRedisDevices = new ArrayList<>();
			for (BlockedDeviceResponse redisDevice : redisDevices) {
				String deviceId = redisDevice.getDeviceId();

				// Check device status in database
				try {
					Map<String, Object> deviceInfo = userServiceRestClient.getDeviceByClientDeviceId(deviceId);
					if (deviceInfo != null) {
						// Device exists in database - check if it's active
						Boolean isActive = deviceInfo.get("isActive") instanceof Boolean
								? (Boolean) deviceInfo.get("isActive")
								: (deviceInfo.get("isActive") != null &&
								deviceInfo.get("isActive").toString().equalsIgnoreCase("true"));

						if (isActive != null && isActive) {
							// Device is active in database, skip it (it was unblocked)
							log.info("Skipping Redis device {} - it's active in database (was unblocked). Redis reason: {}",
									deviceId, redisDevice.getReason());
							continue;
						} else {
							log.debug("Redis device {} is inactive in database, including it in blocked list", deviceId);
						}
					} else {
						log.debug("Redis device {} not found in database, including it in blocked list", deviceId);
					}
				} catch (Exception e) {
					// If we can't check the device status, include it (better safe than sorry)
					log.warn("Could not check device status for Redis device {}: {}. Including it in blocked list.",
							deviceId, e.getMessage());
				}

				// Device is either not in database, inactive, or we couldn't check - include it
				validRedisDevices.add(redisDevice);
			}

			// Merge devices from both sources, avoiding duplicates (by deviceId)
			Map<String, BlockedDeviceResponse> mergedDevices = new HashMap<>();

			// Add valid Redis devices (only those that are not active in database)
			for (BlockedDeviceResponse device : validRedisDevices) {
				mergedDevices.put(device.getDeviceId(), device);
			}

			// Add DB devices (all are inactive by definition)
			for (BlockedDeviceResponse device : dbDevices) {
				mergedDevices.put(device.getDeviceId(), device);
			}

			// Convert to sorted list (by blockedAt descending)
			List<BlockedDeviceResponse> allDevices = new ArrayList<>(mergedDevices.values());
			allDevices.sort((a, b) -> {
				if (a.getBlockedAt() == null && b.getBlockedAt() == null) return 0;
				if (a.getBlockedAt() == null) return 1;
				if (b.getBlockedAt() == null) return -1;
				return b.getBlockedAt().compareTo(a.getBlockedAt()); // Descending order
			});

			// Apply pagination
			int start = (int) pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), allDevices.size());
			List<BlockedDeviceResponse> pageContent = start < allDevices.size()
					? allDevices.subList(start, end)
					: new ArrayList<>();

			return new PageImpl<>(pageContent, pageable, allDevices.size());
		} catch (Exception e) {
			log.error("Error listing blocked devices", e);
			return new PageImpl<>(new ArrayList<>());
		}
	}

	@Override
	public Page<BlockedIpResponse> listBlockedIps(org.springframework.data.domain.Pageable pageable) {
		if (redisTemplate == null) {
			return new PageImpl<>(new ArrayList<>());
		}
		try {
			Set<String> keys = redisTemplate.keys(BLOCKED_IP_PREFIX + "*");
			if (keys == null || keys.isEmpty()) {
				return new PageImpl<>(new ArrayList<>());
			}

			List<BlockedIpResponse> ips = keys.stream()
					.map(key -> {
						String ipAddress = key.substring(BLOCKED_IP_PREFIX.length());
						String reason = redisTemplate.opsForValue().get(key);
						Long ttl = redisTemplate.getExpire(key);

						OffsetDateTime expiresAt = null;
						Boolean isPermanent = true;
						if (ttl != null && ttl > 0) {
							expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(ttl);
							isPermanent = false;
						}

						return BlockedIpResponse.builder()
								.ipAddress(ipAddress)
								.reason(reason)
								.blockedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(ttl != null && ttl > 0 ?
										(365L * 24 * 60 * 60 - ttl) : 0))
								.expiresAt(expiresAt)
								.isPermanent(isPermanent)
								.build();
					})
					.collect(Collectors.toList());

			int start = (int) pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), ips.size());
			List<BlockedIpResponse> pageContent = ips.subList(start, end);

			return new PageImpl<>(pageContent, pageable, ips.size());
		} catch (Exception e) {
			log.error("Error listing blocked IPs", e);
			return new PageImpl<>(new ArrayList<>());
		}
	}

	@Override
	public Page<WhitelistedDeviceResponse> listWhitelistedDevices(org.springframework.data.domain.Pageable pageable) {
		if (redisTemplate == null) {
			return new PageImpl<>(new ArrayList<>());
		}
		try {
			Set<String> keys = redisTemplate.keys(WHITELISTED_DEVICE_PREFIX + "*");
			if (keys == null || keys.isEmpty()) {
				return new PageImpl<>(new ArrayList<>());
			}

			List<WhitelistedDeviceResponse> devices = keys.stream()
					.map(key -> {
						String deviceId = key.substring(WHITELISTED_DEVICE_PREFIX.length());
						String reason = redisTemplate.opsForValue().get(key);
						Long ttl = redisTemplate.getExpire(key);

						OffsetDateTime expiresAt = null;
						Boolean isPermanent = true;
						if (ttl != null && ttl > 0) {
							expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(ttl);
							isPermanent = false;
						}

						return WhitelistedDeviceResponse.builder()
								.deviceId(deviceId)
								.reason(reason)
								.whitelistedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(ttl != null && ttl > 0 ?
										(365L * 24 * 60 * 60 - ttl) : 0))
								.expiresAt(expiresAt)
								.isPermanent(isPermanent)
								.build();
					})
					.collect(Collectors.toList());

			int start = (int) pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), devices.size());
			List<WhitelistedDeviceResponse> pageContent = devices.subList(start, end);

			return new PageImpl<>(pageContent, pageable, devices.size());
		} catch (Exception e) {
			log.error("Error listing whitelisted devices", e);
			return new PageImpl<>(new ArrayList<>());
		}
	}

	@Override
	public Page<WhitelistedIpResponse> listWhitelistedIps(org.springframework.data.domain.Pageable pageable) {
		if (redisTemplate == null) {
			return new PageImpl<>(new ArrayList<>());
		}
		try {
			Set<String> keys = redisTemplate.keys(WHITELISTED_IP_PREFIX + "*");
			if (keys == null || keys.isEmpty()) {
				return new PageImpl<>(new ArrayList<>());
			}

			List<WhitelistedIpResponse> ips = keys.stream()
					.map(key -> {
						String ipAddress = key.substring(WHITELISTED_IP_PREFIX.length());
						String reason = redisTemplate.opsForValue().get(key);
						Long ttl = redisTemplate.getExpire(key);

						OffsetDateTime expiresAt = null;
						Boolean isPermanent = true;
						if (ttl != null && ttl > 0) {
							expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(ttl);
							isPermanent = false;
						}

						return WhitelistedIpResponse.builder()
								.ipAddress(ipAddress)
								.reason(reason)
								.whitelistedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(ttl != null && ttl > 0 ?
										(365L * 24 * 60 * 60 - ttl) : 0))
								.expiresAt(expiresAt)
								.isPermanent(isPermanent)
								.build();
					})
					.collect(Collectors.toList());

			int start = (int) pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), ips.size());
			List<WhitelistedIpResponse> pageContent = ips.subList(start, end);

			return new PageImpl<>(pageContent, pageable, ips.size());
		} catch (Exception e) {
			log.error("Error listing whitelisted IPs", e);
			return new PageImpl<>(new ArrayList<>());
		}
	}
}

