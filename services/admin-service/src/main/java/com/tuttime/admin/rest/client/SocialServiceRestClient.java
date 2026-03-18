package com.tuttime.admin.rest.client;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.dto.PostResponse;
import com.tuttime.admin.dto.CommentResponse;
import com.tuttime.admin.dto.HashtagResponse;
import com.tuttime.admin.dto.PrivacySettingsResponse;
import com.tuttime.admin.dto.UpdatePrivacySettingsRequest;
import com.tuttime.admin.dto.UserSummaryResponse;
import com.tuttime.admin.dto.PostAnalyticsResponse;
import com.tuttime.admin.dto.UserSocialAnalyticsResponse;
import com.tuttime.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Client for Social Service
 * 
 * <p>Used for admin operations to manage social features via social-service REST API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialServiceRestClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.social-service.url:http://localhost:28087}")
    private String socialServiceUrl;

    private WebClient getWebClient() {
        return webClientBuilder.clone()
                .baseUrl(socialServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ==================== Post Management ====================

    public Page<PostResponse> listAllPosts(Pageable pageable, Long userId, Boolean isDeleted, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/api/admin/social/posts")
                                .queryParam("page", pageable.getPageNumber())
                                .queryParam("size", pageable.getPageSize());
                        if (userId != null) {
                            builder.queryParam("userId", userId);
                        }
                        if (isDeleted != null) {
                            builder.queryParam("isDeleted", isDeleted);
                        }
                        return builder.build();
                    })
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            return extractPageFromResponse(response, pageable, this::mapToPostResponse);
        } catch (Exception e) {
            log.error("Error listing posts", e);
            return handleError(e, "Failed to list posts");
        }
    }

    public PostResponse getPostDetails(Long postId, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/social/posts/{postId}", postId)
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToPostResponse(response.getData());
            }
            throw new RuntimeException("Post not found: " + postId);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Post not found: " + postId, e);
        } catch (Exception e) {
            log.error("Error getting post: {}", postId, e);
            throw new RuntimeException("Failed to get post: " + e.getMessage(), e);
        }
    }

    public void deletePost(Long postId, Long adminId) {
        try {
            getWebClient()
                    .delete()
                    .uri("/api/admin/social/posts/{postId}", postId)
                    .header("X-Admin-Id", adminId.toString())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
        } catch (Exception e) {
            log.error("Error deleting post: {}", postId, e);
            throw new RuntimeException("Failed to delete post: " + e.getMessage(), e);
        }
    }

    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/admin/social/posts/user/{userId}")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize())
                            .build(userId))
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            return extractPageFromResponse(response, pageable, this::mapToPostResponse);
        } catch (Exception e) {
            log.error("Error getting user posts: userId={}", userId, e);
            return handleError(e, "Failed to get user posts");
        }
    }

    // ==================== Comment Management ====================

    public Page<CommentResponse> getAllComments(Long postId, Pageable pageable, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/admin/social/posts/{postId}/comments")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize())
                            .build(postId))
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            return extractPageFromResponse(response, pageable, this::mapToCommentResponse);
        } catch (Exception e) {
            log.error("Error getting comments: postId={}", postId, e);
            return handleError(e, "Failed to get comments");
        }
    }

    public CommentResponse getCommentDetails(Long commentId, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/social/comments/{commentId}", commentId)
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToCommentResponse(response.getData());
            }
            throw new RuntimeException("Comment not found: " + commentId);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Comment not found: " + commentId, e);
        } catch (Exception e) {
            log.error("Error getting comment: {}", commentId, e);
            throw new RuntimeException("Failed to get comment: " + e.getMessage(), e);
        }
    }

    public void deleteComment(Long commentId, Long adminId) {
        try {
            getWebClient()
                    .delete()
                    .uri("/api/admin/social/comments/{commentId}", commentId)
                    .header("X-Admin-Id", adminId.toString())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
        } catch (Exception e) {
            log.error("Error deleting comment: {}", commentId, e);
            throw new RuntimeException("Failed to delete comment: " + e.getMessage(), e);
        }
    }

    // ==================== User Blocking Management ====================

    public Page<UserSummaryResponse> getAllBlocks(Long blockerId, Pageable pageable, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/admin/social/blocks")
                            .queryParam("blockerId", blockerId)
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize())
                            .build())
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            return extractPageFromResponse(response, pageable, this::mapToUserSummaryResponse);
        } catch (Exception e) {
            log.error("Error getting blocks: blockerId={}", blockerId, e);
            return handleError(e, "Failed to get blocks");
        }
    }

    public void adminBlockUser(Long blockerId, Long blockedId, Long adminId) {
        try {
            getWebClient()
                    .post()
                    .uri("/api/admin/social/blocks/{blockerId}/{blockedId}", blockerId, blockedId)
                    .header("X-Admin-Id", adminId.toString())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
        } catch (Exception e) {
            log.error("Error blocking user: blockerId={}, blockedId={}", blockerId, blockedId, e);
            throw new RuntimeException("Failed to block user: " + e.getMessage(), e);
        }
    }

    public void adminUnblockUser(Long blockerId, Long blockedId, Long adminId) {
        try {
            getWebClient()
                    .delete()
                    .uri("/api/admin/social/blocks/{blockerId}/{blockedId}", blockerId, blockedId)
                    .header("X-Admin-Id", adminId.toString())
                    .retrieve()
                    .bodyToMono(Void.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();
        } catch (Exception e) {
            log.error("Error unblocking user: blockerId={}, blockedId={}", blockerId, blockedId, e);
            throw new RuntimeException("Failed to unblock user: " + e.getMessage(), e);
        }
    }

    // ==================== Hashtag Management ====================

    public List<HashtagResponse> getAllHashtags(int limit, Long adminId) {
        try {
            ApiResponse<List<Map<String, Object>>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/admin/social/hashtags")
                            .queryParam("limit", limit)
                            .build())
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .map(this::mapToHashtagResponse)
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error getting hashtags", e);
            return List.of();
        }
    }

    public HashtagResponse getHashtagDetails(String tag, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/social/hashtags/{tag}", tag)
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToHashtagResponse(response.getData());
            }
            throw new RuntimeException("Hashtag not found: " + tag);
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("Hashtag not found: " + tag, e);
        } catch (Exception e) {
            log.error("Error getting hashtag: {}", tag, e);
            throw new RuntimeException("Failed to get hashtag: " + e.getMessage(), e);
        }
    }

    public Page<PostResponse> getHashtagPosts(String tag, Pageable pageable, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/admin/social/hashtags/{tag}/posts")
                            .queryParam("page", pageable.getPageNumber())
                            .queryParam("size", pageable.getPageSize())
                            .build(tag))
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            return extractPageFromResponse(response, pageable, this::mapToPostResponse);
        } catch (Exception e) {
            log.error("Error getting hashtag posts: tag={}", tag, e);
            return handleError(e, "Failed to get hashtag posts");
        }
    }

    // ==================== Privacy Settings Management ====================

    public PrivacySettingsResponse getUserPrivacySettings(Long userId, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/social/users/{userId}/privacy", userId)
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToPrivacySettingsResponse(response.getData());
            }
            throw new RuntimeException("Privacy settings not found for user: " + userId);
        } catch (Exception e) {
            log.error("Error getting privacy settings: userId={}", userId, e);
            throw new RuntimeException("Failed to get privacy settings: " + e.getMessage(), e);
        }
    }

    public PrivacySettingsResponse updateUserPrivacySettings(Long userId, UpdatePrivacySettingsRequest request, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .put()
                    .uri("/api/admin/social/users/{userId}/privacy", userId)
                    .header("X-Admin-Id", adminId.toString())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToPrivacySettingsResponse(response.getData());
            }
            throw new RuntimeException("Failed to update privacy settings");
        } catch (Exception e) {
            log.error("Error updating privacy settings: userId={}", userId, e);
            throw new RuntimeException("Failed to update privacy settings: " + e.getMessage(), e);
        }
    }

    // ==================== Analytics & Statistics ====================

    public PostAnalyticsResponse getPostAnalytics(java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path("/api/admin/social/analytics/posts");
                        if (startDate != null) {
                            builder.queryParam("startDate", startDate.toString());
                        }
                        if (endDate != null) {
                            builder.queryParam("endDate", endDate.toString());
                        }
                        return builder.build();
                    })
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToPostAnalyticsResponse(response.getData());
            }
            throw new RuntimeException("Failed to get post analytics");
        } catch (Exception e) {
            log.error("Error getting post analytics", e);
            throw new RuntimeException("Failed to get post analytics: " + e.getMessage(), e);
        }
    }

    public UserSocialAnalyticsResponse getUserSocialAnalytics(Long userId, Long adminId) {
        try {
            ApiResponse<Map<String, Object>> response = getWebClient()
                    .get()
                    .uri("/api/admin/social/analytics/users/{userId}", userId)
                    .header("X-Admin-Id", adminId != null ? adminId.toString() : "")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {})
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                    .block();

            if (response != null && response.getData() != null) {
                return mapToUserSocialAnalyticsResponse(response.getData());
            }
            throw new RuntimeException("User social analytics not found for user: " + userId);
        } catch (Exception e) {
            log.error("Error getting user social analytics: userId={}", userId, e);
            throw new RuntimeException("Failed to get user social analytics: " + e.getMessage(), e);
        }
    }

    // ==================== Helper Methods ====================

    private <T> Page<T> extractPageFromResponse(ApiResponse<Map<String, Object>> response, Pageable pageable, java.util.function.Function<Map<String, Object>, T> mapper) {
        if (response == null || response.getData() == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Map<String, Object> pageData = response.getData();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) pageData.getOrDefault("content", List.of());
        long totalElements = ((Number) pageData.getOrDefault("totalElements", 0L)).longValue();
        int number = ((Number) pageData.getOrDefault("number", pageable.getPageNumber())).intValue();
        int size = ((Number) pageData.getOrDefault("size", pageable.getPageSize())).intValue();

        return new PageImpl<>(
                content.stream().map(mapper).toList(),
                PageRequest.of(number, size),
                totalElements
        );
    }

    private <T> Page<T> handleError(Exception e, String message) {
        if (e instanceof org.springframework.web.reactive.function.client.WebClientRequestException) {
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("Connection refused") ||
                    errorMsg.contains("Connection timed out") ||
                    errorMsg.contains("Service Unavailable"))) {
                log.warn("Social-service is not available. Returning empty page. URL: {}", socialServiceUrl);
                return new PageImpl<>(List.of());
            }
        }
        log.error(message, e);
        throw new RuntimeException(message + ": " + e.getMessage(), e);
    }

    // Mapping methods
    private PostResponse mapToPostResponse(Map<String, Object> data) {
        return PostResponse.builder()
                .postId(getLong(data, "postId"))
                .caption((String) data.get("caption"))
                .likeCount(getLong(data, "likeCount"))
                .commentCount(getLong(data, "commentCount"))
                .isLiked((Boolean) data.get("isLiked"))
                .isSaved((Boolean) data.get("isSaved"))
                .createdAt(parseOffsetDateTime(data.get("createdAt")))
                .updatedAt(parseOffsetDateTime(data.get("updatedAt")))
                .build();
    }

    private CommentResponse mapToCommentResponse(Map<String, Object> data) {
        return CommentResponse.builder()
                .commentId(getLong(data, "commentId"))
                .postId(getLong(data, "postId"))
                .content((String) data.get("content"))
                .likeCount(getLong(data, "likeCount"))
                .isLiked((Boolean) data.get("isLiked"))
                .replyCount(getLong(data, "replyCount"))
                .createdAt(parseOffsetDateTime(data.get("createdAt")))
                .updatedAt(parseOffsetDateTime(data.get("updatedAt")))
                .build();
    }

    private UserSummaryResponse mapToUserSummaryResponse(Map<String, Object> data) {
        String fullName = (String) data.get("fullName");
        String firstName = null;
        String lastName = null;
        if (fullName != null && !fullName.isEmpty()) {
            String[] nameParts = fullName.split(" ", 2);
            firstName = nameParts[0];
            lastName = nameParts.length > 1 ? nameParts[1] : null;
        }
        
        return UserSummaryResponse.builder()
                .userId(getLong(data, "userId"))
                .username((String) data.get("username"))
                .firstName(firstName)
                .lastName(lastName)
                .profileImageUrl((String) data.get("profileImageUrl"))
                .build();
    }

    private HashtagResponse mapToHashtagResponse(Map<String, Object> data) {
        return HashtagResponse.builder()
                .hashtagId(getLong(data, "hashtagId"))
                .tag((String) data.get("tag"))
                .postCount(getLong(data, "postCount"))
                .createdAt(parseOffsetDateTime(data.get("createdAt")))
                .build();
    }

    private PrivacySettingsResponse mapToPrivacySettingsResponse(Map<String, Object> data) {
        return PrivacySettingsResponse.builder()
                .userId(getLong(data, "userId"))
                .accountPrivacy((String) data.get("accountPrivacy"))
                .commentRestriction((String) data.get("commentRestriction"))
                .hideLikes((Boolean) data.get("hideLikes"))
                .hideFollowers((Boolean) data.get("hideFollowers"))
                .createdAt(parseOffsetDateTime(data.get("createdAt")))
                .updatedAt(parseOffsetDateTime(data.get("updatedAt")))
                .build();
    }

    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private java.time.OffsetDateTime parseOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof String) {
            return java.time.OffsetDateTime.parse((String) value);
        }
        return null;
    }

    private PostAnalyticsResponse mapToPostAnalyticsResponse(Map<String, Object> data) {
        return PostAnalyticsResponse.builder()
                .totalPosts(getLong(data, "totalPosts"))
                .totalLikes(getLong(data, "totalLikes"))
                .totalComments(getLong(data, "totalComments"))
                .activeUsers(getLong(data, "activeUsers"))
                .build();
    }

    private UserSocialAnalyticsResponse mapToUserSocialAnalyticsResponse(Map<String, Object> data) {
        return UserSocialAnalyticsResponse.builder()
                .totalPosts(getLong(data, "totalPosts"))
                .totalFollowers(getLong(data, "totalFollowers"))
                .totalFollowing(getLong(data, "totalFollowing"))
                .totalLikesReceived(getLong(data, "totalLikesReceived"))
                .totalCommentsReceived(getLong(data, "totalCommentsReceived"))
                .build();
    }
}

