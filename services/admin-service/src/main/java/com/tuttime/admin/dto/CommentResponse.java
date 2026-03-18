package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for comment details (admin view)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    
    private Long commentId;
    private Long postId;
    private UserSummaryResponse author;
    private String content;
    private Long parentCommentId;
    private Long likeCount;
    private Boolean isLiked;
    private Long replyCount;
    private List<CommentResponse> replies;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

