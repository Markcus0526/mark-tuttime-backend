package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.rest.client.UserServiceRestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Admin User Management Service Interface
 * 
 * <p>Provides management features for regular users (not admin users).
 * This service delegates to user-service via REST client.
 */
public interface AdminUserManagementService {
    
    Page<UserSummaryResponse> listUsers(String search, String status, String kycStatus, Pageable pageable);
    UserDetailResponse getUserDetails(Long userId);
    UserDetailResponse updateUser(Long userId, UpdateUserRequest request);
    UserDetailResponse blockUser(Long userId, String reason);
    UserDetailResponse unblockUser(Long userId);
    UserDetailResponse approveKyc(Long userId);
    UserDetailResponse rejectKyc(Long userId, String reason);
    void deleteUser(Long userId);
    ReferralNetworkResponse getUserReferrals(Long userId);
    String createReferral(Long referrerId, Long refereeId, Integer level);
    BulkOperationResponse bulkBlockUsers(BulkOperationRequest request);
    BulkOperationResponse bulkUnblockUsers(BulkOperationRequest request);
    BulkOperationResponse bulkApproveKyc(BulkOperationRequest request);
    BulkOperationResponse bulkRejectKyc(BulkOperationRequest request);
    BulkOperationResponse bulkDeleteUsers(BulkOperationRequest request);
    Map<String, Object> getUserOverview(Long userId, Integer conversionPage, Integer conversionSize,
                                       Integer transactionPage, Integer transactionSize,
                                       Integer boostPage, Integer boostSize);
    Map<String, Object> getExtendedUserDetail(Long userId);
    void resetMining(Long userId, Map<String, Object> request);
    void pauseAccount(Long userId, Map<String, Object> request);
    void unpauseAccount(Long userId);
    Map<String, Object> addBonus(Long userId, Map<String, Object> request);
    void freezeWithdrawal(Long userId, Map<String, Object> request);
    void unfreezeWithdrawal(Long userId);
    void markAntiFraud(Long userId, Map<String, Object> request);
    void unmarkAntiFraud(Long userId);
    Map<String, Object> deleteBetaTesterByPhone(String phoneNumber);
}
