package com.tuttime.review.service.impl;

import com.tuttime.review.dto.ReviewDto;
import com.tuttime.review.entity.Review;
import com.tuttime.review.mapper.ReviewMapper;
import com.tuttime.review.repository.ReviewRepository;
import com.tuttime.review.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;
	private final ReviewMapper reviewMapper;

	public ReviewServiceImpl(ReviewRepository reviewRepository, ReviewMapper reviewMapper) {
		this.reviewRepository = reviewRepository;
		this.reviewMapper = reviewMapper;
	}

	@Override
	public ReviewDto getReviewById(UUID id) {
		Review review = reviewRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Review not found with id " + id));
		return reviewMapper.toDTO(review);
	}

	@Override
	public List<ReviewDto> getReviewsByRestaurant(UUID restaurantId) {
		List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);
		return reviews.stream()
				.map(reviewMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ReviewDto> getReviewsByUser(UUID userId) {
		List<Review> reviews = reviewRepository.findByUserId(userId);
		return reviews.stream()
				.map(reviewMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ReviewDto> getReviewsByStatus(String status) {
		List<Review> reviews = reviewRepository.findByStatus(status);
		return reviews.stream()
				.map(reviewMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ReviewDto createReview(ReviewDto reviewDto) {
		reviewDto.setStatus("PENDING");
		Review review = reviewMapper.toEntity(reviewDto);
		Review saved = reviewRepository.save(review);
		return reviewMapper.toDTO(saved);
	}

	@Override
	public ReviewDto updateReview(UUID id, ReviewDto reviewDto) {
		Review existing = reviewRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Review not found with id " + id));
		existing.setRating(reviewDto.getRating());
		existing.setComment(reviewDto.getComment());
		existing.setStatus(reviewDto.getStatus());
		Review updated = reviewRepository.save(existing);
		return reviewMapper.toDTO(updated);
	}

	@Override
	public void deleteReview(UUID id) {
		if (!reviewRepository.existsById(id)) {
			throw new EntityNotFoundException("Review not found with id " + id);
		}
		reviewRepository.deleteById(id);
	}
}

