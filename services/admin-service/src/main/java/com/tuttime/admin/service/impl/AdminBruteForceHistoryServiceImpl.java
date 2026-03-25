package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BruteForceHistoryResponse;
import com.tuttime.admin.dto.WarningSeverity;
import com.tuttime.admin.service.AdminBruteForceHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Admin Brute-Force History Service Implementation
 *
 * <p>Retrieves brute-force history from Redis and converts to history entries.
 * Note: Since brute-force data is stored in Redis with TTL, historical data
 * may be limited. For complete history, consider persisting to database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBruteForceHistoryServiceImpl implements AdminBruteForceHistoryService {

	@Autowired(required = false)
	private RedisTemplate<String, String> redisTemplate;

	// Redis key prefixes for brute-force data
	private static final String BRUTEFORCE_ATTEMPT_PREFIX = "bruteforce:login:";
	private static final String BRUTEFORCE_BLOCKED_IP_PREFIX = "bruteforce:blocked:ip:";
	private static final String BRUTEFORCE_BLOCKED_PHONE_PREFIX = "bruteforce:blocked:phone:";

	@Override
	public Page<BruteForceHistoryResponse> getBruteForceHistory(
			Pageable pageable,
			OffsetDateTime startDate,
			OffsetDateTime endDate,
			String ipAddress,
			String phoneNumber) {

		log.debug("Getting brute-force history: startDate={}, endDate={}, ipAddress={}, phoneNumber={}",
				startDate, endDate, ipAddress, phoneNumber);

		List<BruteForceHistoryResponse> history = new ArrayList<>();

		if (redisTemplate == null) {
			log.warn("Redis not available - returning empty brute-force history");
			return new PageImpl<>(history, pageable, 0);
		}

		try {
			// Get blocked IPs
			Set<String> blockedIpKeys = redisTemplate.keys(BRUTEFORCE_BLOCKED_IP_PREFIX + "*");
			if (blockedIpKeys != null) {
				for (String key : blockedIpKeys) {
					String ip = key.substring(BRUTEFORCE_BLOCKED_IP_PREFIX.length());

					// Apply IP filter if specified
					if (ipAddress != null && !ipAddress.isEmpty() && !ip.equals(ipAddress)) {
						continue;
					}

					String blockUntilStr = redisTemplate.opsForValue().get(key);
					if (blockUntilStr != null) {
						try {
							long blockUntil = Long.parseLong(blockUntilStr);
							long now = System.currentTimeMillis() / 1000;

							OffsetDateTime detectedAt = OffsetDateTime.ofInstant(
									Instant.ofEpochSecond(blockUntil - (15 * 60), 0), ZoneOffset.UTC); // Approximate detection time
							OffsetDateTime blockedUntil = OffsetDateTime.ofInstant(
									Instant.ofEpochSecond(blockUntil, 0), ZoneOffset.UTC);

							// Apply date filters
							if (startDate != null && detectedAt.isBefore(startDate)) {
								continue;
							}
							if (endDate != null && detectedAt.isAfter(endDate)) {
								continue;
							}

							// Determine attempt count and severity
							long attempts = getAttemptCount(ip, null);
							WarningSeverity severity = determineSeverity(attempts);

							history.add(BruteForceHistoryResponse.builder()
									.historyId("BF_IP_" + ip + "_" + blockUntil)
									.ipAddress(ip)
									.phoneNumber(null)
									.attemptCount(attempts)
									.severity(severity)
									.action(blockUntil > now + (10 * 365 * 24 * 60 * 60) ? "PERMANENT_BLOCK" : "BLOCKED_IP")
									.detectedAt(detectedAt)
									.blockedUntil(blockedUntil)
									.isPermanent(blockUntil > now + (10 * 365 * 24 * 60 * 60))
									.description(String.format("IP address %s blocked due to %d failed login attempts", ip, attempts))
									.build());
						} catch (NumberFormatException e) {
							log.warn("Invalid block timestamp for IP: {}", ip, e);
						}
					}
				}
			}

			// Get blocked phone numbers
			Set<String> blockedPhoneKeys = redisTemplate.keys(BRUTEFORCE_BLOCKED_PHONE_PREFIX + "*");
			if (blockedPhoneKeys != null) {
				for (String key : blockedPhoneKeys) {
					String phone = key.substring(BRUTEFORCE_BLOCKED_PHONE_PREFIX.length());

					// Apply phone filter if specified
					if (phoneNumber != null && !phoneNumber.isEmpty() && !phone.equals(phoneNumber)) {
						continue;
					}

					String blockUntilStr = redisTemplate.opsForValue().get(key);
					if (blockUntilStr != null) {
						try {
							long blockUntil = Long.parseLong(blockUntilStr);
							long now = System.currentTimeMillis() / 1000;

							OffsetDateTime detectedAt = OffsetDateTime.ofInstant(
									Instant.ofEpochSecond(blockUntil - (60 * 60), 0), ZoneOffset.UTC); // Approximate detection time
							OffsetDateTime blockedUntil = OffsetDateTime.ofInstant(
									Instant.ofEpochSecond(blockUntil, 0), ZoneOffset.UTC);

							// Apply date filters
							if (startDate != null && detectedAt.isBefore(startDate)) {
								continue;
							}
							if (endDate != null && detectedAt.isAfter(endDate)) {
								continue;
							}

							// Determine attempt count and severity
							long attempts = getAttemptCount(null, phone);
							WarningSeverity severity = determineSeverity(attempts);

							history.add(BruteForceHistoryResponse.builder()
									.historyId("BF_PHONE_" + phone + "_" + blockUntil)
									.ipAddress(null)
									.phoneNumber(phone)
									.attemptCount(attempts)
									.severity(severity)
									.action(blockUntil > now + (10 * 365 * 24 * 60 * 60) ? "PERMANENT_BLOCK" : "BLOCKED_PHONE")
									.detectedAt(detectedAt)
									.blockedUntil(blockedUntil)
									.isPermanent(blockUntil > now + (10 * 365 * 24 * 60 * 60))
									.description(String.format("Phone number %s blocked due to %d failed login attempts", phone, attempts))
									.build());
						} catch (NumberFormatException e) {
							log.warn("Invalid block timestamp for phone: {}", phone, e);
						}
					}
				}
			}

			// Get active attempt counters (warnings but not yet blocked)
			Set<String> attemptKeys = redisTemplate.keys(BRUTEFORCE_ATTEMPT_PREFIX + "*");
			if (attemptKeys != null) {
				for (String key : attemptKeys) {
					String[] parts = key.substring(BRUTEFORCE_ATTEMPT_PREFIX.length()).split(":");
					if (parts.length >= 2) {
						String ip = parts[0];
						String phone = parts[1];

						// Apply filters
						if (ipAddress != null && !ipAddress.isEmpty() && !ip.equals(ipAddress)) {
							continue;
						}
						if (phoneNumber != null && !phoneNumber.isEmpty() && !phone.equals(phoneNumber)) {
							continue;
						}

						String value = redisTemplate.opsForValue().get(key);
						if (value != null) {
							try {
								long attempts = Long.parseLong(value);
								if (attempts >= 3) { // Only include warnings
									OffsetDateTime detectedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1); // Approximate

									// Apply date filters
									if (startDate != null && detectedAt.isBefore(startDate)) {
										continue;
									}
									if (endDate != null && detectedAt.isAfter(endDate)) {
										continue;
									}

									WarningSeverity severity = determineSeverity(attempts);

									history.add(BruteForceHistoryResponse.builder()
											.historyId("BF_ATTEMPT_" + key.hashCode())
											.ipAddress(ip)
											.phoneNumber(phone)
											.attemptCount(attempts)
											.severity(severity)
											.action("WARNING")
											.detectedAt(detectedAt)
											.blockedUntil(null)
											.isPermanent(false)
											.description(String.format("Multiple failed login attempts detected: %d attempts from IP %s", attempts, ip))
											.build());
								}
							} catch (NumberFormatException e) {
								log.warn("Invalid attempt count for key: {}", key, e);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			log.error("Error fetching brute-force history from Redis", e);
		}

		// Sort by detected date (newest first)
		history.sort((a, b) -> b.getDetectedAt().compareTo(a.getDetectedAt()));

		// Apply pagination
		int start = (int) pageable.getOffset();
		int end = Math.min((start + pageable.getPageSize()), history.size());
		List<BruteForceHistoryResponse> pageContent = start < history.size()
				? history.subList(start, end)
				: new ArrayList<>();

		return new PageImpl<>(pageContent, pageable, history.size());
	}

	/**
	 * Get attempt count for IP and/or phone
	 */
	private long getAttemptCount(String ipAddress, String phoneNumber) {
		if (redisTemplate == null) {
			return 0;
		}

		try {
			if (ipAddress != null && phoneNumber != null) {
				String attemptKey = BRUTEFORCE_ATTEMPT_PREFIX + ipAddress + ":" + phoneNumber;
				String value = redisTemplate.opsForValue().get(attemptKey);
				if (value != null) {
					return Long.parseLong(value);
				}
			}
		} catch (Exception e) {
			log.warn("Error getting attempt count", e);
		}

		return 0;
	}

	/**
	 * Determine severity based on attempt count
	 */
	private WarningSeverity determineSeverity(long attempts) {
		if (attempts >= 15) {
			return WarningSeverity.CRITICAL;
		} else if (attempts >= 10) {
			return WarningSeverity.HIGH;
		} else if (attempts >= 5) {
			return WarningSeverity.MEDIUM;
		} else {
			return WarningSeverity.LOW;
		}
	}
}

