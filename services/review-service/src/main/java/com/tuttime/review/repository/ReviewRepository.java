package com.tuttime.review.repository;

import com.tuttime.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
	List<Review> findByRestaurantId(UUID restaurantId);
	List<Review> findByUserId(UUID userId);
	List<Review> findByStatus(String status);
}
