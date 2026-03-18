package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;
import org.springframework.data.domain.Page;

/**
 * Admin Mining Service Interface
 */
public interface AdminMiningService {
    Page<ApBalanceResponse> listApBalances(String search, org.springframework.data.domain.Pageable pageable);
    ApBalanceResponse getUserApBalance(Long userId);
    Page<MiningSessionResponse> getMiningSessions(Long userId, org.springframework.data.domain.Pageable pageable);
    MiningSpeedResponse getMiningSpeed(Long userId);
    MiningStatusResponse getMiningStatus(Long userId);
}

