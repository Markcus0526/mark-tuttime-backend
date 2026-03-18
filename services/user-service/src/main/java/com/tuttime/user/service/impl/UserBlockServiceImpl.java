package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.kafka.EventProducer;
import com.tuttime.common.kafka.UserBlockEvent;
import com.tuttime.user.dto.BlockedUserResponse;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserBlock;
import com.tuttime.user.repository.UserBlockRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.service.UserBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for managing user blocks using PostgreSQL database
 * 
 * <p>All users can block/unblock other users and view their blocked list.
 * Uses soft delete (deletedAt) to allow unblocking while preserving history.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {
    
    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;
    
    @Autowired(required = false)
    private CacheManager cacheManager;
    /** Event producer for publishing user blocking events to Kafka (optional - only available when Kafka is enabled) */
    @Autowired(required = false)
    private EventProducer eventProducer;

    @Value("${spring.application.name:user-service}")
    private String serviceName;
    
    @Override
    @Transactional
    public void blockUser(Long blockerUserId, Long blockedUserId) {
        if (blockerUserId.equals(blockedUserId)) {
            throw new BusinessException("Cannot block yourself");
        }
        
        // Validate users exist
        userRepository.findByIdAndNotDeleted(blockerUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Blocker user not found"));
        userRepository.findByIdAndNotDeleted(blockedUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Blocked user not found"));
        
        // Check if block already exists (active)
        Optional<UserBlock> existingBlock = userBlockRepository
            .findByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(blockerUserId, blockedUserId);
        
        if (existingBlock.isPresent()) {
            log.debug("User {} already has an active block for user {}", blockerUserId, blockedUserId);
            return; // Block already exists, no need to create again
        }
        
        // Check if there's a soft-deleted block (unblocked previously) - restore it
        Optional<UserBlock> softDeletedBlock = userBlockRepository
            .findByBlockerUserIdAndBlockedUserId(blockerUserId, blockedUserId)
            .filter(block -> block.getDeletedAt() != null);
        
        boolean blockStatusChanged = false;
        if (softDeletedBlock.isPresent()) {
            // Restore the block by clearing deletedAt
            UserBlock block = softDeletedBlock.get();
            block.setDeletedAt(null);
            block.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            userBlockRepository.save(block);
            blockStatusChanged = true;
            log.info("User {} re-blocked user {} (restored previous block)", blockerUserId, blockedUserId);
        } else {
            // Create new block
            UserBlock block = UserBlock.builder()
                .blockerUserId(blockerUserId)
                .blockedUserId(blockedUserId)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .deletedAt(null)
                .build();
            userBlockRepository.save(block);
            blockStatusChanged = true;
            log.info("User {} blocked user {}", blockerUserId, blockedUserId);
        }
        
        // Evict cache after transaction commits to ensure database changes are visible
        if (blockStatusChanged) {
            final Long userIdToEvict = blockedUserId;
            TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        evictUserCache(userIdToEvict);
                    }
                }
            );
            log.info("User {} blocked user {} - cache will be evicted after transaction commit", blockerUserId, blockedUserId);
        }

        // Publish user block event to Kafka (for other services to sync)
        publishUserBlockEvent(blockerUserId, blockedUserId, true);
    }
    
    @Override
    @Transactional
    public void unblockUser(Long blockerUserId, Long blockedUserId) {
        // Find active block
        Optional<UserBlock> block = userBlockRepository
            .findByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(blockerUserId, blockedUserId);
        
        if (block.isEmpty()) {
            log.debug("No active block found for user {} blocking user {}", blockerUserId, blockedUserId);
            return; // Block doesn't exist or already unblocked
        }
        
        // Soft delete the block
        UserBlock userBlock = block.get();
        userBlock.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        userBlock.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        userBlockRepository.save(userBlock);
        
        // Evict cache after transaction commits to ensure database changes are visible
        final Long userIdToEvict = blockedUserId;
        TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    evictUserCache(userIdToEvict);
                }
            }
        );
        
        log.info("User {} unblocked user {} - cache will be evicted after transaction commit", blockerUserId, blockedUserId);

        // Publish user unblock event to Kafka (for other services to sync)
        publishUserBlockEvent(blockerUserId, blockedUserId, false);
    }
    
    /**
     * Evicts the cache for a user's data.
     * This ensures that getUserById returns fresh data after block/unblock operations.
     * Also evicts map-queries cache to ensure map screen shows updated block status.
     */
    private void evictUserCache(Long userId) {
        if (cacheManager == null) {
            log.warn("CacheManager is not available - cannot evict cache for user {}", userId);
            return;
        }
        
        try {
            // Evict user cache
            Cache usersCache = cacheManager.getCache("users");
            if (usersCache != null) {
                usersCache.evict(userId);
                log.info("Evicted 'users' cache for user {}", userId);
            } else {
                log.warn("Cache 'users' not found in CacheManager");
            }
            
            // Evict map-queries cache (all entries) since block status affects map results
            Cache mapQueriesCache = cacheManager.getCache("map-queries");
            if (mapQueriesCache != null) {
                mapQueriesCache.clear();
                log.info("Evicted all 'map-queries' cache entries");
            } else {
                log.debug("Cache 'map-queries' not found in CacheManager");
            }
        } catch (Exception e) {
            log.error("Failed to evict cache for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long userId1, Long userId2) {
        // Check bidirectional: if either user blocked the other
        Optional<UserBlock> block = userBlockRepository.findBidirectionalBlock(userId1, userId2);
        boolean blocked = block.isPresent();
        log.debug("Checking if user {} blocked user {}: {} (block present: {})", 
            userId1, userId2, blocked, block.isPresent());
        return blocked;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasBlocked(Long blockerUserId, Long blockedUserId) {
        // Check one-way: if blockerUserId has blocked blockedUserId
        boolean hasBlocked = userBlockRepository
            .findByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(blockerUserId, blockedUserId)
            .isPresent();
        log.debug("Checking if user {} has blocked user {}: {}", blockerUserId, blockedUserId, hasBlocked);
        return hasBlocked;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds(Long userId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerUserIdAndDeletedAtIsNull(userId);
        return blocks.stream()
            .map(UserBlock::getBlockedUserId)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Set<Long> getBlockedUserIdsSet(Long userId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerUserIdAndDeletedAtIsNull(userId);
        return blocks.stream()
            .map(UserBlock::getBlockedUserId)
            .collect(Collectors.toSet());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers(Long userId) {
        List<UserBlock> blocks = userBlockRepository.findByBlockerUserIdAndDeletedAtIsNull(userId);
        if (blocks.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Fetch user details - only return username for privacy
        List<BlockedUserResponse> responses = new ArrayList<>();
        for (UserBlock block : blocks) {
            try {
                User user = userRepository.findByIdAndNotDeleted(block.getBlockedUserId()).orElse(null);
                if (user != null) {
                    // Privacy requirement: Only return username (name), not userId or blockedAt
                    responses.add(BlockedUserResponse.builder()
                        .username(user.getUsername())
                        .build());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch blocked user {}: {}", block.getBlockedUserId(), e.getMessage());
            }
        }
        
        return responses;
    }

    /**
     * Publishes user blocking/unblocking event to Kafka for other services to sync.
     *
     * @param blockerUserId User ID who is blocking/unblocking
     * @param blockedUserId User ID being blocked/unblocked
     * @param isBlock true if blocking, false if unblocking
     */
    private void publishUserBlockEvent(Long blockerUserId, Long blockedUserId, boolean isBlock) {
        if (eventProducer != null) {
            try {
                UserBlockEvent event = UserBlockEvent.builder()
                        .eventType(isBlock ? UserBlockEvent.BlockEventType.USER_BLOCKED : UserBlockEvent.BlockEventType.USER_UNBLOCKED)
                        .blockerUserId(blockerUserId)
                        .blockedUserId(blockedUserId)
                        .sourceService(serviceName)
                        .blockContext("MAP") // Blocking from map/user-service
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .build();

                eventProducer.publishUserBlockEvent(event);
                log.debug("Published user {} event: blocker={}, blocked={}, source={}", 
                        isBlock ? "block" : "unblock", blockerUserId, blockedUserId, serviceName);
            } catch (Exception e) {
                log.error("Failed to publish user block event: blocker={}, blocked={}, isBlock={}", 
                        blockerUserId, blockedUserId, isBlock, e);
                // Don't throw - event publishing failure shouldn't break blocking operation
            }
        } else {
            log.debug("EventProducer not available - user block event not published");
        }
    }
}

