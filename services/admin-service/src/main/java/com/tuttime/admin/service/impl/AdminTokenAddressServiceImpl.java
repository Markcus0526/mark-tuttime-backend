package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BlockedAddressResponse;
import com.tuttime.admin.dto.WhitelistedAddressResponse;
import com.tuttime.admin.service.AdminTokenAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Admin Token Address Service Implementation
 *
 * <p>Manages blocking and whitelisting of token/wallet addresses using Redis.
 * Addresses are stored with metadata including reason and admin who performed the action.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenAddressServiceImpl implements AdminTokenAddressService {

	@Autowired(required = false)
	private RedisTemplate<String, String> redisTemplate;

	private static final String BLOCKED_ADDRESS_PREFIX = "admin:blocked:address:";
	private static final String WHITELISTED_ADDRESS_PREFIX = "admin:whitelisted:address:";
	private static final String ADDRESS_METADATA_SEPARATOR = "|";

	@Override
	public Page<BlockedAddressResponse> listBlockedAddresses(Pageable pageable) {
		if (redisTemplate == null) {
			return new PageImpl<>(new ArrayList<>());
		}
		try {
			Set<String> keys = redisTemplate.keys(BLOCKED_ADDRESS_PREFIX + "*");
			if (keys == null || keys.isEmpty()) {
				return new PageImpl<>(new ArrayList<>());
			}

			List<BlockedAddressResponse> addresses = keys.stream()
					.map(key -> {
						String address = key.substring(BLOCKED_ADDRESS_PREFIX.length());
						String value = redisTemplate.opsForValue().get(key);
						Long ttl = redisTemplate.getExpire(key);

						String[] parts = value != null ? value.split("\\" + ADDRESS_METADATA_SEPARATOR, 3) : new String[3];
						String reason = parts.length > 0 ? parts[0] : null;
						String blockedBy = parts.length > 1 ? parts[1] : null;

						OffsetDateTime expiresAt = null;
						Boolean isPermanent = true;
						if (ttl != null && ttl > 0) {
							expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(ttl);
							isPermanent = false;
						}

						return BlockedAddressResponse.builder()
								.address(address)
								.reason(reason)
								.blockedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(ttl != null && ttl > 0 ?
										(365L * 24 * 60 * 60 - ttl) : 0))
								.expiresAt(expiresAt)
								.isPermanent(isPermanent)
								.blockedBy(blockedBy)
								.build();
					})
					.collect(Collectors.toList());

			int start = (int) pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), addresses.size());
			List<BlockedAddressResponse> pageContent = addresses.subList(start, end);

			return new PageImpl<>(pageContent, pageable, addresses.size());
		} catch (Exception e) {
			log.error("Error listing blocked addresses", e);
			return new PageImpl<>(new ArrayList<>());
		}
	}

	@Override
	public Page<WhitelistedAddressResponse> listWhitelistedAddresses(Pageable pageable) {
		if (redisTemplate == null) {
			return new PageImpl<>(new ArrayList<>());
		}
		try {
			Set<String> keys = redisTemplate.keys(WHITELISTED_ADDRESS_PREFIX + "*");
			if (keys == null || keys.isEmpty()) {
				return new PageImpl<>(new ArrayList<>());
			}

			List<WhitelistedAddressResponse> addresses = keys.stream()
					.map(key -> {
						String address = key.substring(WHITELISTED_ADDRESS_PREFIX.length());
						String value = redisTemplate.opsForValue().get(key);
						Long ttl = redisTemplate.getExpire(key);

						String[] parts = value != null ? value.split("\\" + ADDRESS_METADATA_SEPARATOR, 3) : new String[3];
						String reason = parts.length > 0 ? parts[0] : null;
						String whitelistedBy = parts.length > 1 ? parts[1] : null;

						OffsetDateTime expiresAt = null;
						Boolean isPermanent = true;
						if (ttl != null && ttl > 0) {
							expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(ttl);
							isPermanent = false;
						}

						return WhitelistedAddressResponse.builder()
								.address(address)
								.reason(reason)
								.whitelistedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(ttl != null && ttl > 0 ?
										(365L * 24 * 60 * 60 - ttl) : 0))
								.expiresAt(expiresAt)
								.isPermanent(isPermanent)
								.whitelistedBy(whitelistedBy)
								.build();
					})
					.collect(Collectors.toList());

			int start = (int) pageable.getOffset();
			int end = Math.min((start + pageable.getPageSize()), addresses.size());
			List<WhitelistedAddressResponse> pageContent = addresses.subList(start, end);

			return new PageImpl<>(pageContent, pageable, addresses.size());
		} catch (Exception e) {
			log.error("Error listing whitelisted addresses", e);
			return new PageImpl<>(new ArrayList<>());
		}
	}

	@Override
	public void blockAddress(String address, String reason, Long adminId) {
		if (redisTemplate == null) {
			log.warn("Redis not available - address blocking skipped");
			return;
		}
		try {
			String key = BLOCKED_ADDRESS_PREFIX + address.toLowerCase();
			// Store: reason|adminId|timestamp
			String value = (reason != null ? reason : "Admin blocked") +
					ADDRESS_METADATA_SEPARATOR +
					(adminId != null ? adminId.toString() : "system") +
					ADDRESS_METADATA_SEPARATOR +
					System.currentTimeMillis();
			// Block for 1 year (effectively permanent until manually unblocked)
			redisTemplate.opsForValue().set(key, value, 365, TimeUnit.DAYS);
			log.info("Address blocked: address={}, reason={}, adminId={}", address, reason, adminId);
		} catch (Exception e) {
			log.error("Error blocking address: address={}", address, e);
			throw new RuntimeException("Failed to block address: " + e.getMessage(), e);
		}
	}

	@Override
	public void unblockAddress(String address) {
		if (redisTemplate == null) {
			return;
		}
		try {
			String key = BLOCKED_ADDRESS_PREFIX + address.toLowerCase();
			redisTemplate.delete(key);
			log.info("Address unblocked: address={}", address);
		} catch (Exception e) {
			log.error("Error unblocking address: address={}", address, e);
			throw new RuntimeException("Failed to unblock address: " + e.getMessage(), e);
		}
	}

	@Override
	public void whitelistAddress(String address, String reason, Long adminId) {
		if (redisTemplate == null) {
			log.warn("Redis not available - address whitelisting skipped");
			return;
		}
		try {
			String key = WHITELISTED_ADDRESS_PREFIX + address.toLowerCase();
			// Store: reason|adminId|timestamp
			String value = (reason != null ? reason : "Admin whitelisted") +
					ADDRESS_METADATA_SEPARATOR +
					(adminId != null ? adminId.toString() : "system") +
					ADDRESS_METADATA_SEPARATOR +
					System.currentTimeMillis();
			// Whitelist for 1 year (effectively permanent until manually removed)
			redisTemplate.opsForValue().set(key, value, 365, TimeUnit.DAYS);
			log.info("Address whitelisted: address={}, reason={}, adminId={}", address, reason, adminId);
		} catch (Exception e) {
			log.error("Error whitelisting address: address={}", address, e);
			throw new RuntimeException("Failed to whitelist address: " + e.getMessage(), e);
		}
	}

	@Override
	public void removeAddressFromWhitelist(String address) {
		if (redisTemplate == null) {
			return;
		}
		try {
			String key = WHITELISTED_ADDRESS_PREFIX + address.toLowerCase();
			redisTemplate.delete(key);
			log.info("Address removed from whitelist: address={}", address);
		} catch (Exception e) {
			log.error("Error removing address from whitelist: address={}", address, e);
			throw new RuntimeException("Failed to remove address from whitelist: " + e.getMessage(), e);
		}
	}
}

