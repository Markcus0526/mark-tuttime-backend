package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.grpc.client.TokenServiceGrpcClient;
import com.tuttime.admin.grpc.client.UserServiceGrpcClient;
import com.tuttime.admin.rest.client.TokenServiceRestClient;
import com.tuttime.admin.service.AdminTokenService;
import com.tuttime.common.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.tuttime.common.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Token Service Implementation
 * 
 * <p>Integrates with token-service via gRPC to provide token management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenServiceImpl implements AdminTokenService {

    private final TokenServiceGrpcClient tokenServiceGrpcClient;
    private final TokenServiceRestClient tokenServiceRestClient;
    private final UserServiceGrpcClient userServiceGrpcClient;

    @Override
    public TokenBalanceResponse getUserTokenBalance(Long userId) {
        // Token-service not deployed - return zero balance immediately
        log.debug("Token-service not deployed - returning zero balance for user: {}", userId);
        try {
            // Get user info for username
            UserResponse user = userServiceGrpcClient.getUserById(userId);
            String firstName = user != null && user.getFirstName() != null ? user.getFirstName() : "";
            String lastName = user != null && user.getLastName() != null ? user.getLastName() : "";
            String username = (firstName + " " + lastName).trim();
            if (username.isEmpty() && user != null && user.getUsername() != null) {
                username = user.getUsername();
            }
            
            return TokenBalanceResponse.builder()
                    .userId(userId)
                    .username(username)
                    .balance(BigDecimal.ZERO)
                    .lockedAmount(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to get user info for userId={}, returning zero balance with empty username. Error: {}", userId, e.getMessage());
            return TokenBalanceResponse.builder()
                    .userId(userId)
                    .username("")
                    .balance(BigDecimal.ZERO)
                    .lockedAmount(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .build();
        }
    }
    
    /**
     * Helper method to parse BigDecimal from Map value
     */
    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            try {
                return new BigDecimal((String) value);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse BigDecimal from string: {}", value);
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    @Override
    public TokenBalanceResponse adjustTokenBalance(Long userId, AdjustTokenBalanceRequest request) {
        // Token-service not deployed - return zero balance immediately
        log.warn("Token-service not deployed - cannot adjust token balance for userId={}, returning zero balance", userId);
        return getUserTokenBalance(userId);
    }

    @Override
    public Page<TokenTransactionResponse> getTokenTransactions(
            Long userId, 
            String type, 
            String status, 
            org.springframework.data.domain.Pageable pageable) {
        // Token-service not deployed - return empty page immediately
        log.debug("Token-service not deployed - returning empty transactions page for user: {}", userId);
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }
    
    /**
     * Parses a datetime value to OffsetDateTime.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param dateValue The datetime value to parse (String or null)
     * @return Parsed OffsetDateTime in UTC context, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        if (dateValue instanceof String) {
            try {
                // Try ISO format first - parse as OffsetDateTime and ensure UTC
                return OffsetDateTime.parse((String) dateValue).withOffsetSameInstant(ZoneOffset.UTC);
            } catch (Exception e) {
                log.warn("Failed to parse date: {}", dateValue, e);
                return null;
            }
        }
        return null;
    }

    @Override
    public TokenBalanceResponse burnTokens(Long userId, com.tuttime.admin.dto.BurnTokensRequest request) {
        // Token-service not deployed - return zero balance immediately
        log.warn("Token-service not deployed - cannot burn tokens for userId={}, returning zero balance", userId);
        return getUserTokenBalance(userId);
    }

    @Override
    public com.tuttime.admin.dto.UserStatusResponse getUserStatus(Long userId) {
        // Token-service not deployed - return default status immediately
        log.debug("Token-service not deployed - returning default status for user: {}", userId);
        return com.tuttime.admin.dto.UserStatusResponse.builder()
                .userId(userId)
                .statusLevel("BRONZE")
                .statusBonusApPerHour(BigDecimal.ZERO)
                .build();
    }

    @Override
    public BulkOperationResponse bulkAdjustTokenBalances(BulkTokenAdjustmentRequest request) {
        // Token-service not deployed - return all failures
        log.warn("Token-service not deployed - cannot adjust token balances in bulk");
        if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
            throw new BusinessException("User IDs are required");
        }
        
        List<BulkOperationResponse.BulkOperationResult> results = new ArrayList<>();
        for (Long userId : request.getUserIds()) {
            results.add(BulkOperationResponse.BulkOperationResult.builder()
                    .userId(userId)
                    .success(false)
                    .errorMessage("Token service is not available")
                    .build());
        }
        
        return BulkOperationResponse.success(request.getUserIds().size(), 0, results);
    }

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public Page<GlobalTransactionResponse> getDepositHistory(String status, String network, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable) {
        // Token-service not deployed - return empty page immediately
        log.debug("Token-service not deployed - returning empty deposit history");
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }

    @Override
    public Page<GlobalTransactionResponse> getWithdrawalHistory(String status, String network, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable) {
        // Token-service not deployed - return empty page immediately
        log.debug("Token-service not deployed - returning empty withdrawal history");
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }

    @Override
    public Page<GlobalTransactionResponse> getBurnHistory(OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable) {
        // Token-service not deployed - return empty page immediately
        log.debug("Token-service not deployed - returning empty burn history");
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }

    @Override
    public Page<StakingClaimResponse> getClaimHistory(Long userId, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable) {
        // Token-service not deployed - return empty page immediately
        log.debug("Token-service not deployed - returning empty claim history for user: {}", userId);
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }

    /**
     * Parses a datetime string to OffsetDateTime.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param dateTimeStr The datetime string to parse (ISO format, UTC)
     * @return Parsed OffsetDateTime in UTC context, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            // Parse as OffsetDateTime and ensure UTC timezone
            return OffsetDateTime.parse(dateTimeStr).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * Helper method to parse OffsetDateTime from Map value (can be String, OffsetDateTime, or null).
     * 
     * <p><b>Timezone Note:</b> Parsed dates are converted to UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param dateValue The datetime value to parse (OffsetDateTime, String, or null)
     * @return Parsed OffsetDateTime in UTC timezone, or null if parsing fails
     */
    private OffsetDateTime parseDateTimeFromMap(Object dateValue) {
        if (dateValue == null) {
            return null;
        }
        if (dateValue instanceof OffsetDateTime) {
            return ((OffsetDateTime) dateValue).withOffsetSameInstant(ZoneOffset.UTC);
        }
        if (dateValue instanceof String) {
            // Delegates to parseDateTime which handles UTC parsing
            return parseDateTime((String) dateValue);
        }
        return null;
    }
}

