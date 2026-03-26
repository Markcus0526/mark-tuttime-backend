package com.tuttime.review.service.impl;

import com.tuttime.review.dto.RatingAggregateDto;
import com.tuttime.review.entity.RatingAggregate;
import com.tuttime.review.mapper.RatingAggregateMapper;
import com.tuttime.review.repository.RatingAggregateRepository;
import com.tuttime.review.service.RatingAggregateService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RatingAggregateServiceImpl implements RatingAggregateService {

	private final RatingAggregateRepository ratingAggregateRepository;
	private final RatingAggregateMapper ratingAggregateMapper;

	public RatingAggregateServiceImpl(RatingAggregateRepository ratingAggregateRepository,
	                                  RatingAggregateMapper ratingAggregateMapper) {
		this.ratingAggregateRepository = ratingAggregateRepository;
		this.ratingAggregateMapper = ratingAggregateMapper;
	}

	@Override
	public RatingAggregateDto getByRestaurantId(UUID restaurantId) {
		RatingAggregate aggregate = ratingAggregateRepository.findById(restaurantId)
				.orElseThrow(() -> new EntityNotFoundException("Rating aggregate not found for restaurant " + restaurantId));
		return ratingAggregateMapper.toDTO(aggregate);
	}

	@Override
	public RatingAggregateDto createOrUpdate(RatingAggregateDto dto) {
		RatingAggregate entity = ratingAggregateMapper.toEntity(dto);
		RatingAggregate saved = ratingAggregateRepository.save(entity);
		return ratingAggregateMapper.toDTO(saved);
	}

	@Override
	public void delete(UUID restaurantId) {
		if (!ratingAggregateRepository.existsById(restaurantId)) {
			throw new EntityNotFoundException("Rating aggregate not found for restaurant " + restaurantId);
		}
		ratingAggregateRepository.deleteById(restaurantId);
	}
}
