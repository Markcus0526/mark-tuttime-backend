package com.tuttime.review.service.impl;

import com.tuttime.review.dto.ReviewDTO;
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
	public ReviewDTO getReviewById(UUID id) {
		Review review = reviewRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Review not found with id " + id));
		return reviewMapper.toDTO(review);
	}

	@Override
	public List<ReviewDTO> getReviewsByRestaurant(UUID restaurantId) {
		List<Review> reviews = reviewRepository.findByRestaurantId(restaurantId);
		return reviews.stream()
				.map(reviewMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ReviewDTO> getReviewsByUser(UUID userId) {
		List<Review> reviews = reviewRepository.findByUserId(userId);
		return reviews.stream()
				.map(reviewMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ReviewDTO> getReviewsByStatus(String status) {
		List<Review> reviews = reviewRepository.findByStatus(status);
		return reviews.stream()
				.map(reviewMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ReviewDTO createReview(ReviewDTO reviewDTO) {
		reviewDTO.setStatus("PENDING");
		Review review = reviewMapper.toEntity(reviewDTO);
		Review saved = reviewRepository.save(review);
		return reviewMapper.toDTO(saved);
	}

	@Override
	public ReviewDTO updateReview(UUID id, ReviewDTO reviewDTO) {
		Review existing = reviewRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Review not found with id " + id));
		existing.setRating(reviewDTO.getRating());
		existing.setComment(reviewDTO.getComment());
		existing.setStatus(reviewDTO.getStatus());
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

