package com.tuttime.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

/**
 * User Block Entity - Tracks user-to-user blocking relationships
 * 
 * <p>This entity maps to the `user_blocks` table in the database.
 * It stores information about which users have blocked other users.
 * 
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Soft delete support (deleted_at for unblocking)</li>
 *   <li>Bidirectional block checking support</li>
 *   <li>Audit timestamps (created_at, updated_at)</li>
 *   <li>Prevents self-blocking (database constraint)</li>
 * </ul>
 * 
 * <p><b>Database Table:</b>
 * <p>Maps to `user_blocks` table in `user_service` schema.
 * Uses soft delete (deleted_at) to allow unblocking without losing history.
 */
@Entity
@Table(name = "user_blocks", schema = "user_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserBlock {
    
    /** Unique identifier for the block record (auto-generated, primary key) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long blockId;
    
    /** User ID who is blocking (foreign key to users table) */
    @Column(name = "blocker_user_id", nullable = false)
    private Long blockerUserId;
    
    /** User ID who is being blocked (foreign key to users table) */
    @Column(name = "blocked_user_id", nullable = false)
    private Long blockedUserId;
    
    /** Timestamp when the block was created */
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;
    
    /** Timestamp when the record was last updated (auto-updated) */
    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;
    
    /** Timestamp when the block was removed (soft delete for unblocking) */
    @Column(name = "deleted_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime deletedAt;
}

