package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.rest.client.SocialServiceRestClient;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminSocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin Social Service Implementation
 *
 * <p>Orchestrates admin operations for social features via social-service REST API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSocialServiceImpl implements AdminSocialService {

	private final SocialServiceRestClient socialServiceRestClient;
	private final AdminAuditService auditService;

	// ==================== Post Management ====================

	@Override
	public Page<PostResponse> listAllPosts(Pageable pageable, Long userId, Boolean isDeleted) {
		log.debug("Listing all posts: userId={}, isDeleted={}", userId, isDeleted);
		return socialServiceRestClient.listAllPosts(pageable, userId, isDeleted, null);
	}

	@Override
	public PostResponse getPostDetails(Long postId) {
		log.debug("Getting post details: postId={}", postId);
		return socialServiceRestClient.getPostDetails(postId, null);
	}

	@Override
	public void deletePost(Long postId, Long adminId) {
		log.info("Admin {} deleting post {}", adminId, postId);
		socialServiceRestClient.deletePost(postId, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.POST_DELETED,
				"POST", postId, "Admin deleted post");
	}

	@Override
	public Page<PostResponse> getUserPosts(Long userId, Pageable pageable) {
		log.debug("Getting posts for user: userId={}", userId);
		return socialServiceRestClient.getUserPosts(userId, pageable, null);
	}

	// ==================== Comment Management ====================

	@Override
	public Page<CommentResponse> getAllComments(Long postId, Pageable pageable) {
		log.debug("Getting all comments for post: postId={}", postId);
		return socialServiceRestClient.getAllComments(postId, pageable, null);
	}

	@Override
	public CommentResponse getCommentDetails(Long commentId) {
		log.debug("Getting comment details: commentId={}", commentId);
		return socialServiceRestClient.getCommentDetails(commentId, null);
	}

	@Override
	public void deleteComment(Long commentId, Long adminId) {
		log.info("Admin {} deleting comment {}", adminId, commentId);
		socialServiceRestClient.deleteComment(commentId, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.COMMENT_DELETED,
				"COMMENT", commentId, "Admin deleted comment");
	}

	// ==================== User Blocking Management ====================

	@Override
	public Page<UserSummaryResponse> getAllBlocks(Long blockerId, Pageable pageable) {
		log.debug("Getting blocks for user: blockerId={}", blockerId);
		return socialServiceRestClient.getAllBlocks(blockerId, pageable, null);
	}

	@Override
	public void adminBlockUser(Long blockerId, Long blockedId, Long adminId) {
		log.info("Admin {} blocking user {} on behalf of user {}", adminId, blockedId, blockerId);
		socialServiceRestClient.adminBlockUser(blockerId, blockedId, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_BLOCKED,
				"USER_BLOCK", blockedId, "Admin blocked user on behalf of user " + blockerId);
	}

	@Override
	public void adminUnblockUser(Long blockerId, Long blockedId, Long adminId) {
		log.info("Admin {} unblocking user {} on behalf of user {}", adminId, blockedId, blockerId);
		socialServiceRestClient.adminUnblockUser(blockerId, blockedId, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UNBLOCKED,
				"USER_BLOCK", blockedId, "Admin unblocked user on behalf of user " + blockerId);
	}

	// ==================== Hashtag Management ====================

	@Override
	public List<HashtagResponse> getAllHashtags(int limit) {
		log.debug("Getting all hashtags: limit={}", limit);
		return socialServiceRestClient.getAllHashtags(limit, null);
	}

	@Override
	public HashtagResponse getHashtagDetails(String tag) {
		log.debug("Getting hashtag details: tag={}", tag);
		return socialServiceRestClient.getHashtagDetails(tag, null);
	}

	@Override
	public Page<PostResponse> getHashtagPosts(String tag, Pageable pageable) {
		log.debug("Getting posts for hashtag: tag={}", tag);
		return socialServiceRestClient.getHashtagPosts(tag, pageable, null);
	}

	// ==================== Privacy Settings Management ====================

	@Override
	public PrivacySettingsResponse getUserPrivacySettings(Long userId) {
		log.debug("Getting privacy settings for user: userId={}", userId);
		return socialServiceRestClient.getUserPrivacySettings(userId, null);
	}

	@Override
	public PrivacySettingsResponse updateUserPrivacySettings(Long userId, UpdatePrivacySettingsRequest request, Long adminId) {
		log.info("Admin {} updating privacy settings for user {}", adminId, userId);
		PrivacySettingsResponse response = socialServiceRestClient.updateUserPrivacySettings(userId, request, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
				"PRIVACY_SETTINGS", userId, "Admin updated privacy settings");
		return response;
	}

	// ==================== Analytics & Statistics ====================

	@Override
	public PostAnalyticsResponse getPostAnalytics(java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate) {
		log.debug("Getting post analytics: startDate={}, endDate={}", startDate, endDate);
		return socialServiceRestClient.getPostAnalytics(startDate, endDate, null);
	}

	@Override
	public UserSocialAnalyticsResponse getUserSocialAnalytics(Long userId) {
		log.debug("Getting user social analytics: userId={}", userId);
		return socialServiceRestClient.getUserSocialAnalytics(userId, null);
	}
}

