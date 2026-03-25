package com.tuttime.admin.service;

import com.tuttime.admin.dto.ConversionInfoResponse;
import com.tuttime.admin.dto.ConversionLedgerEntry;
import com.tuttime.admin.dto.ConversionResponse;
import org.springframework.data.domain.Page;

/**
 * Admin Conversion Service Interface
 */
public interface AdminConversionService {
	Page<ConversionResponse> getUserConversions(Long userId, org.springframework.data.domain.Pageable pageable);

	ConversionInfoResponse getConversionInfo(Long userId);

	Page<ConversionLedgerEntry> getMonthlyConversionLedger(int year, int month, org.springframework.data.domain.Pageable pageable);
}

