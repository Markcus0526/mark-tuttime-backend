package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.dto.PostAnalyticsResponse;
import com.tuttime.admin.dto.UserSocialAnalyticsResponse;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminSocialService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Admin Social Management Controller
 * 
 * <p>Provides admin endpoints for managing social features:
 * - Post moderation (view, delete)
 * - Comment moderation (view, delete)
 * - User blocking management
 * - Hashtag management
 * - Privacy settings management
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/social")
@RequiredArgsConstructor
@Tag(name = "Admin Social Management", description = "Admin APIs for managing social features")
public class AdminSocialController {

    private final AdminSocialService socialService;
    private final AdminAuditService auditService;

    // ==================== Post Management ====================

    @GetMapping("/posts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List all posts", description = "Get paginated list of all posts with optional filters")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> listAllPosts(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean isDeleted,
            @AuthenticationPrincipal Long adminId) {
        
        Page<PostResponse> posts = socialService.listAllPosts(pageable, userId, isDeleted);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.POST_VIEWED,
                "POST", null, "Listed all posts");
        
        return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", posts));
    }

    @GetMapping("/posts/{postId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get post details", description = "Get detailed information about a post")
    public ResponseEntity<ApiResponse<PostResponse>> getPostDetails(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long adminId) {
        
        PostResponse post = socialService.getPostDetails(postId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.POST_VIEWED,
                "POST", postId, "Viewed post details");
        
        return ResponseEntity.ok(ApiResponse.success("Post retrieved successfully", post));
    }

    @DeleteMapping("/posts/{postId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Delete post", description = "Admin can delete any post")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long adminId) {
        
        socialService.deletePost(postId, adminId);
        return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", null));
    }

    @GetMapping("/posts/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user posts", description = "Get all posts by a specific user")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getUserPosts(
            @PathVariable Long userId,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<PostResponse> posts = socialService.getUserPosts(userId, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.POST_VIEWED,
                "POST", null, "Viewed posts for user " + userId);
        
        return ResponseEntity.ok(ApiResponse.success("User posts retrieved successfully", posts));
    }

    // ==================== Comment Management ====================

    @GetMapping("/posts/{postId}/comments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get all comments for a post", description = "Get all comments including deleted ones")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getAllComments(
            @PathVariable Long postId,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<CommentResponse> comments = socialService.getAllComments(postId, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.COMMENT_VIEWED,
                "COMMENT", null, "Viewed comments for post " + postId);
        
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", comments));
    }

    @GetMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get comment details", description = "Get detailed information about a comment")
    public ResponseEntity<ApiResponse<CommentResponse>> getCommentDetails(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long adminId) {
        
        CommentResponse comment = socialService.getCommentDetails(commentId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.COMMENT_VIEWED,
                "COMMENT", commentId, "Viewed comment details");
        
        return ResponseEntity.ok(ApiResponse.success("Comment retrieved successfully", comment));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Delete comment", description = "Admin can delete any comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long adminId) {
        
        socialService.deleteComment(commentId, adminId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }

    // ==================== User Blocking Management ====================

    @GetMapping("/blocks")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List all blocks", description = "Get paginated list of all user blocks")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> getAllBlocks(
            @RequestParam Long blockerId,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<UserSummaryResponse> blockedUsers = socialService.getAllBlocks(blockerId, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_BLOCK_VIEWED,
                "USER_BLOCK", blockerId, "Viewed blocks for user");
        
        return ResponseEntity.ok(ApiResponse.success("Blocks retrieved successfully", blockedUsers));
    }

    @PostMapping("/blocks/{blockerId}/{blockedId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Block user", description = "Admin can block a user on behalf of another user")
    public ResponseEntity<ApiResponse<Void>> adminBlockUser(
            @PathVariable Long blockerId,
            @PathVariable Long blockedId,
            @AuthenticationPrincipal Long adminId) {
        
        socialService.adminBlockUser(blockerId, blockedId, adminId);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/blocks/{blockerId}/{blockedId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Unblock user", description = "Admin can unblock a user on behalf of another user")
    public ResponseEntity<ApiResponse<Void>> adminUnblockUser(
            @PathVariable Long blockerId,
            @PathVariable Long blockedId,
            @AuthenticationPrincipal Long adminId) {
        
        socialService.adminUnblockUser(blockerId, blockedId, adminId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    // ==================== Hashtag Management ====================

    @GetMapping("/hashtags")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List all hashtags", description = "Get list of all hashtags")
    public ResponseEntity<ApiResponse<List<HashtagResponse>>> getAllHashtags(
            @RequestParam(defaultValue = "100") int limit,
            @AuthenticationPrincipal Long adminId) {
        
        List<HashtagResponse> hashtags = socialService.getAllHashtags(limit);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.HASHTAG_VIEWED,
                "HASHTAG", null, "Listed all hashtags");
        
        return ResponseEntity.ok(ApiResponse.success("Hashtags retrieved successfully", hashtags));
    }

    @GetMapping("/hashtags/{tag}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get hashtag details", description = "Get detailed information about a hashtag")
    public ResponseEntity<ApiResponse<HashtagResponse>> getHashtagDetails(
            @PathVariable String tag,
            @AuthenticationPrincipal Long adminId) {
        
        HashtagResponse hashtag = socialService.getHashtagDetails(tag);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.HASHTAG_VIEWED,
                "HASHTAG", hashtag.getHashtagId(), "Viewed hashtag details");
        
        return ResponseEntity.ok(ApiResponse.success("Hashtag retrieved successfully", hashtag));
    }

    @GetMapping("/hashtags/{tag}/posts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get posts by hashtag", description = "Get all posts using a specific hashtag")
    public ResponseEntity<ApiResponse<Page<PostResponse>>> getHashtagPosts(
            @PathVariable String tag,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<PostResponse> posts = socialService.getHashtagPosts(tag, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.POST_VIEWED,
                "POST", null, "Viewed posts for hashtag " + tag);
        
        return ResponseEntity.ok(ApiResponse.success("Hashtag posts retrieved successfully", posts));
    }

    // ==================== Privacy Settings Management ====================

    @GetMapping("/users/{userId}/privacy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user privacy settings", description = "View privacy settings for any user")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> getUserPrivacySettings(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        PrivacySettingsResponse settings = socialService.getUserPrivacySettings(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.PRIVACY_SETTINGS_VIEWED,
                "PRIVACY_SETTINGS", userId, "Viewed privacy settings");
        
        return ResponseEntity.ok(ApiResponse.success("Privacy settings retrieved successfully", settings));
    }

    @PutMapping("/users/{userId}/privacy")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Update user privacy settings", description = "Admin can update privacy settings for any user")
    public ResponseEntity<ApiResponse<PrivacySettingsResponse>> updateUserPrivacySettings(
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePrivacySettingsRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        PrivacySettingsResponse settings = socialService.updateUserPrivacySettings(userId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Privacy settings updated successfully", settings));
    }

    // ==================== Analytics & Statistics ====================

    @GetMapping("/analytics/posts")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get post analytics", description = "Get statistics about posts")
    public ResponseEntity<ApiResponse<PostAnalyticsResponse>> getPostAnalytics(
            @RequestParam(required = false) OffsetDateTime startDate,
            @RequestParam(required = false) OffsetDateTime endDate,
            @AuthenticationPrincipal Long adminId) {
        
        PostAnalyticsResponse analytics = socialService.getPostAnalytics(startDate, endDate);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ANALYTICS_VIEWED,
                "ANALYTICS", null, "Viewed post analytics");
        
        return ResponseEntity.ok(ApiResponse.success("Post analytics retrieved successfully", analytics));
    }

    @GetMapping("/analytics/users/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user social analytics", description = "Get social statistics for a user")
    public ResponseEntity<ApiResponse<UserSocialAnalyticsResponse>> getUserSocialAnalytics(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        UserSocialAnalyticsResponse analytics = socialService.getUserSocialAnalytics(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ANALYTICS_VIEWED,
                "ANALYTICS", userId, "Viewed user social analytics");
        
        return ResponseEntity.ok(ApiResponse.success("User social analytics retrieved successfully", analytics));
    }
}

