package com.tuttime.review.repository;

import com.tuttime.review.entity.RatingAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RatingAggregateRepository extends JpaRepository<RatingAggregate, UUID> {
	// JpaRepository provides CRUD methods including findById, save, delete, etc.
}