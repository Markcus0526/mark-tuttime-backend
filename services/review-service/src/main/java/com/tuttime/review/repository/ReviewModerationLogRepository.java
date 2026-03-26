package com.tuttime.review.repository;

import com.tuttime.review.entity.ReviewModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewModerationLogRepository extends JpaRepository<ReviewModerationLog, UUID> {
	List<ReviewModerationLog> findByReviewId(UUID reviewId);
	List<ReviewModerationLog> findByModeratorId(UUID moderatorId);
}
