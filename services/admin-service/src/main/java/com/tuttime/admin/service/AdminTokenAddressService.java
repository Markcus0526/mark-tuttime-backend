package com.tuttime.admin.service;

import com.tuttime.admin.dto.BlockedAddressResponse;
import com.tuttime.admin.dto.WhitelistedAddressResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin Token Address Service Interface
 * Manages blocking and whitelisting of token/wallet addresses
 */
public interface AdminTokenAddressService {
	Page<BlockedAddressResponse> listBlockedAddresses(Pageable pageable);

	Page<WhitelistedAddressResponse> listWhitelistedAddresses(Pageable pageable);

	void blockAddress(String address, String reason, Long adminId);

	void unblockAddress(String address);

	void whitelistAddress(String address, String reason, Long adminId);

	void removeAddressFromWhitelist(String address);
}

