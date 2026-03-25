package com.tuttime.admin.service;

import com.tuttime.admin.dto.AdRequest;
import com.tuttime.admin.dto.AdResponse;
import com.tuttime.admin.dto.RejectRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin Ad Service Interface
 */
public interface AdminAdService {
	Page<AdResponse> listAds(Pageable pageable, String status);

	AdResponse getAd(Long id);

	AdResponse createAd(AdRequest request);

	AdResponse updateAd(Long id, AdRequest request);

	void deleteAd(Long id);

	AdResponse approveAd(Long id, Long adminId);

	AdResponse rejectAd(Long id, Long adminId, RejectRequest request);
}

