package com.tuttime.user.service.query;

import com.tuttime.common.proto.*;
import com.tuttime.user.dto.ReferralInfoResponse;
import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.readmodel.UserReadModel;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.repository.readmodel.UserReadModelRepository;
import com.tuttime.user.rest.client.RewardsServiceRestClient;
import com.tuttime.user.grpc.client.MiningServiceGrpcClient;
import com.tuttime.user.service.ImageUrlCacheService;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/** CQRS Query Service: optimized user reads using read model. */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {
    private final UserRepository userRepository;
    private final UserReadModelRepository userReadModelRepository;
    private final UserMapper userMapper;
    private final MiningServiceGrpcClient miningServiceGrpcClient;
    private final RewardsServiceRestClient rewardsServiceRestClient;
    private final ImageUrlCacheService imageUrlCacheService;
    private final CacheManager cacheManager;

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    @NewSpan("get-user-by-id")
    public UserResponse getUserById(@SpanTag("user.id") Long userId) {
        log.debug("Query: Fetching user with ID: {} from read model (includes location)", userId);

        // Try to get from read model first (optimized path)
        Optional<UserReadModel> readModelOpt = userReadModelRepository.findByIdAndNotDeleted(userId);
        
        User user = userRepository.findById(userId).orElse(null);
        
        // Check if user exists (even if blocked or not in read model)
        if (user == null || user.getDeletedAt() != null) {
            throw new com.tuttime.common.exception.ResourceNotFoundException("User not found with id: " + userId);
        }
        
        UserResponse response;
        
        if (readModelOpt.isPresent()) {
            // Use read model (fast path)
            UserReadModel readModel = readModelOpt.get();
            response = userMapper.toResponse(readModel);
            // Explicitly set balances from read model (MapStruct should map automatically, but ensure it's set)
            response.setTotalApartBalance(readModel.getTotalApartBalance());
            response.setTotalApBalance(readModel.getTotalApBalance());
        } else {
            // Fallback: user exists but not in read model (e.g., blocked users)
            // Create response from main User entity
            log.debug("User {} not found in read model, using main User table (may be blocked)", userId);
            response = userMapper.toResponse(user);
            // Set default values for balances when read model is not available
            response.setTotalApartBalance(BigDecimal.ZERO);
            response.setTotalApBalance(BigDecimal.ZERO);
        }
        
        // Always enrich with data from main User table (for blocked users, etc.)
        response.setPhone(user.getPhone());
        response.setTelegram(user.getTelegram());
        response.setCountry(user.getCountry());
        response.setCity(user.getCity());
        response.setKycVerifiedAt(user.getKycVerifiedAt());
        response.setBlockedReason(user.getBlockedReason());
        response.setBlockedAt(user.getBlockedAt());
        response.setIsBlocked(user.getIsBlocked());
        
        // Enrich with generated image URL from key
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        
        enrichWithMiningAndFriendsData(response, userId);
        
        return response;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#email", unless = "#result == null")
    @NewSpan("get-user-by-email")
    public UserResponse getUserByEmail(@SpanTag("user.email") String email) {
        log.debug("Query: Fetching user with email: {} from read model (includes location)", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("User not found with email: " + email));

        UserReadModel readModel = userReadModelRepository.findByEmail(email)
                .orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("User not found with email: " + email));
        
        UserResponse response = userMapper.toResponse(readModel);
        response.setPhone(user.getPhone());
        response.setTelegram(user.getTelegram());
        response.setCountry(user.getCountry());
        response.setCity(user.getCity());
        response.setKycVerifiedAt(user.getKycVerifiedAt());
        response.setBlockedReason(user.getBlockedReason());
        response.setBlockedAt(user.getBlockedAt());
        
        // Enrich with generated image URL from key
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        
        return response;
    }

    /**
     * Programmatic cache handling to avoid Integer→Long ClassCastException.
     * Redis/Jackson deserialises small numbers as Integer, but the return type is Long.
     * Manual caching lets us call Number.longValue() safely on any cached numeric type.
     */
    @Transactional(readOnly = true)
    @NewSpan("get-user-id-by-username")
    public Long getUserIdByUsername(@SpanTag("user.username") String username) {
        log.debug("Query: Fetching user ID by username: {}", username);

        String cacheKey = "username:" + username;
        Cache cache = cacheManager.getCache("users");

        // Check cache first
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null && cached.get() != null) {
                Object value = cached.get();
                if (value instanceof Number) {
                    return ((Number) value).longValue();   // safe for Integer, Long, etc.
                }
            }
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new com.tuttime.common.exception.ResourceNotFoundException("User not found with username: " + username));

        Long userId = user.getUserId();

        if (cache != null) {
            cache.put(cacheKey, userId);
        }

        return userId;
    }

    /** Enriches user response with mining speed, AP balance, and friends count from external services. */
    private void enrichWithMiningAndFriendsData(UserResponse response, Long userId) {
        // Fetch current mining speed
        try {
            GetMiningSpeedResponse miningSpeedResponse = miningServiceGrpcClient.getMiningSpeed(userId);
            if (miningSpeedResponse != null) {
                response.setCurrentMiningSpeed(BigDecimal.valueOf(miningSpeedResponse.getTotalSpeed()));
            } else {
                response.setCurrentMiningSpeed(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.warn("Failed to get mining speed for user {}: {}", userId, e.getMessage());
            response.setCurrentMiningSpeed(BigDecimal.ZERO);
        }
        
        // Fetch current AP balance
        try {
            ApBalanceResponse apBalanceResponse = miningServiceGrpcClient.getApBalance(userId);
            if (apBalanceResponse != null) {
                response.setCurrentApBalance(new BigDecimal(apBalanceResponse.getCurrentBalance()));
            } else {
                response.setCurrentApBalance(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.warn("Failed to get AP balance for user {}: {}", userId, e.getMessage());
            response.setCurrentApBalance(BigDecimal.ZERO);
        }
        
        // Fetch total friends count (total referrals)
        try {
            ReferralInfoResponse referralInfo = rewardsServiceRestClient.getReferralInfo(userId);
            if (referralInfo != null && referralInfo.getTotalReferrals() != null) {
                response.setTotalFriendsCount(referralInfo.getTotalReferrals());
            } else {
                response.setTotalFriendsCount(0);
            }
        } catch (Exception e) {
            log.warn("Failed to get friends count for user {}: {}", userId, e.getMessage());
            response.setTotalFriendsCount(0);
        }
    }

}

