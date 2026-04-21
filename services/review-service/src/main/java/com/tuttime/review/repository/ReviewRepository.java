package com.tuttime.review.repository;

import com.tuttime.review.entity.Review;
import com.tuttime.review.entity.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

	// Spring generates the SQL:
	// SELECT * FROM reviews WHERE restaurant_id = ? AND status = ?
	Page<Review> findByBranchIdAndStatus(UUID restaurantId, ReviewStatus status, Pageable pageable);

	/**
	 * Fetches approved reviews for a restaurant with Pagination.
	 * Perfect for infinite scroll on the Mobile App.
	 */
	Page<Review> findByBranchIdAndStatusOrderByCreatedAtDesc(
			UUID branchId,
			ReviewStatus status,
			Pageable pageable
	);

	/**
	 * Used by the Admin Panel to find reviews requiring moderation.
	 */
	List<Review> findByStatus(ReviewStatus status);

	/**
	 * Prevents a user from spamming multiple reviews for the same restaurant.
	 */
	boolean existsByUserIdAndBranchId(UUID userId, UUID branchId);
}
