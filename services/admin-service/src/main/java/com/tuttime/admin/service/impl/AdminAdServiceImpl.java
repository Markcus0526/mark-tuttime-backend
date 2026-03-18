package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.AdRequest;
import com.tuttime.admin.dto.AdResponse;
import com.tuttime.admin.rest.client.ContentServiceRestClient;
import com.tuttime.admin.service.AdminAdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Admin Ad Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAdServiceImpl implements AdminAdService {

    private final ContentServiceRestClient contentServiceRestClient;

    @Override
    public Page<AdResponse> listAds(Pageable pageable, String status) {
        return contentServiceRestClient.listAds(pageable, status);
    }

    @Override
    public AdResponse getAd(Long id) {
        return contentServiceRestClient.getAd(id);
    }

    @Override
    public AdResponse createAd(AdRequest request) {
        return contentServiceRestClient.createAd(request);
    }

    @Override
    public AdResponse updateAd(Long id, AdRequest request) {
        return contentServiceRestClient.updateAd(id, request);
    }

    @Override
    public void deleteAd(Long id) {
        contentServiceRestClient.deleteAd(id);
    }

    @Override
    public AdResponse approveAd(Long id, Long adminId) {
        return contentServiceRestClient.approveAd(id, adminId);
    }

    @Override
    public AdResponse rejectAd(Long id, Long adminId, com.tuttime.admin.dto.RejectRequest request) {
        return contentServiceRestClient.rejectAd(id, adminId, request);
    }
}

