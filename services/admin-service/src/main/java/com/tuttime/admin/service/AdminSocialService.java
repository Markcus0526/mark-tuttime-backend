package com.tuttime.admin.service;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.dto.PostAnalyticsResponse;
import com.tuttime.admin.dto.UserSocialAnalyticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin Social Service Interface
 * 
 * <p>Service for managing social features from admin panel.
 * Integrates with social-service via REST.
 */
public interface AdminSocialService {
    
    // ==================== Post Management ====================
    
    Page<PostResponse> listAllPosts(Pageable pageable, Long userId, Boolean isDeleted);
    
    PostResponse getPostDetails(Long postId);
    
    void deletePost(Long postId, Long adminId);
    
    Page<PostResponse> getUserPosts(Long userId, Pageable pageable);
    
    // ==================== Comment Management ====================
    
    Page<CommentResponse> getAllComments(Long postId, Pageable pageable);
    
    CommentResponse getCommentDetails(Long commentId);
    
    void deleteComment(Long commentId, Long adminId);
    
    // ==================== User Blocking Management ====================
    
    Page<UserSummaryResponse> getAllBlocks(Long blockerId, Pageable pageable);
    
    void adminBlockUser(Long blockerId, Long blockedId, Long adminId);
    
    void adminUnblockUser(Long blockerId, Long blockedId, Long adminId);
    
    // ==================== Hashtag Management ====================
    
    List<HashtagResponse> getAllHashtags(int limit);
    
    HashtagResponse getHashtagDetails(String tag);
    
    Page<PostResponse> getHashtagPosts(String tag, Pageable pageable);
    
    // ==================== Privacy Settings Management ====================
    
    PrivacySettingsResponse getUserPrivacySettings(Long userId);
    
    PrivacySettingsResponse updateUserPrivacySettings(Long userId, UpdatePrivacySettingsRequest request, Long adminId);
    
    // ==================== Analytics & Statistics ====================
    
    PostAnalyticsResponse getPostAnalytics(java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate);
    
    UserSocialAnalyticsResponse getUserSocialAnalytics(Long userId);
}

