package com.tuttime.user.repository;

import com.tuttime.user.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UserBlock entity
 * 
 * <p>Provides data access methods for user-to-user blocking relationships.
 * Uses soft delete (deletedAt) to track unblocked users while preserving history.
 */
@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    
    /**
     * Find all active blocks by a user (where user is the blocker)
     * 
     * @param blockerUserId User ID who is blocking
     * @return List of active blocks where this user is the blocker
     */
    List<UserBlock> findByBlockerUserIdAndDeletedAtIsNull(Long blockerUserId);
    
    /**
     * Find specific block relationship
     * 
     * @param blockerUserId User ID who is blocking
     * @param blockedUserId User ID who is being blocked
     * @return Optional containing the block if found and active
     */
    Optional<UserBlock> findByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(
        Long blockerUserId, 
        Long blockedUserId
    );
    
    /**
     * Check if a block relationship exists (active)
     * 
     * @param blockerUserId User ID who is blocking
     * @param blockedUserId User ID who is being blocked
     * @return true if block exists and is active, false otherwise
     */
    boolean existsByBlockerUserIdAndBlockedUserIdAndDeletedAtIsNull(
        Long blockerUserId, 
        Long blockedUserId
    );
    
    /**
     * Check bidirectional block (if either user blocked the other)
     * 
     * <p>This checks if there's a block relationship in either direction.
     * Used to determine if two users have blocked each other.
     * 
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return Optional containing the block if either user blocked the other
     */
    @Query("SELECT b FROM UserBlock b WHERE " +
           "((b.blockerUserId = :userId1 AND b.blockedUserId = :userId2) OR " +
           "(b.blockerUserId = :userId2 AND b.blockedUserId = :userId1)) " +
           "AND b.deletedAt IS NULL")
    Optional<UserBlock> findBidirectionalBlock(
        @Param("userId1") Long userId1, 
        @Param("userId2") Long userId2
    );
    
    /**
     * Count active blocks by a user (how many users this user has blocked)
     * 
     * @param blockerUserId User ID who is blocking
     * @return Number of active blocks
     */
    long countByBlockerUserIdAndDeletedAtIsNull(Long blockerUserId);
    
    /**
     * Find all users who have blocked a specific user
     * 
     * @param blockedUserId User ID who is being blocked
     * @return List of active blocks where this user is being blocked
     */
    List<UserBlock> findByBlockedUserIdAndDeletedAtIsNull(Long blockedUserId);
    
    /**
     * Find block by blocker and blocked user IDs (including soft-deleted)
     * Used to check if a block exists (active or soft-deleted) for restore functionality
     * 
     * @param blockerUserId User ID who is blocking
     * @param blockedUserId User ID who is being blocked
     * @return Optional containing the block if found (active or soft-deleted)
     */
    Optional<UserBlock> findByBlockerUserIdAndBlockedUserId(Long blockerUserId, Long blockedUserId);
}

