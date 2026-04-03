package com.tuttime.review.repository;

import com.tuttime.review.entity.ReviewModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModerationLogRepository extends JpaRepository<ReviewModerationLog, UUID> {

	/**
	 * Retrieves all moderation actions taken on a specific review.
	 * Useful for dispute resolution in the Admin Panel.
	 */
	List<ReviewModerationLog> findByReviewIdOrderByCreatedAtDesc(UUID reviewId);

	/**
	 * Tracks performance of a specific moderator.
	 */
	List<ReviewModerationLog> findByModeratorId(UUID moderatorId);
}
