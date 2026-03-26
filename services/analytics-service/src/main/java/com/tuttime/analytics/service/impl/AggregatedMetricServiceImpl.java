package com.tuttime.analytics.service.impl;

import com.tuttime.analytics.dto.AggregatedMetricDto;
import com.tuttime.analytics.entity.AggregatedMetric;
import com.tuttime.analytics.mapper.AggregatedMetricMapper;
import com.tuttime.analytics.repository.AggregatedMetricRepository;
import com.tuttime.analytics.service.AggregatedMetricService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AggregatedMetricServiceImpl implements AggregatedMetricService {

	private final AggregatedMetricRepository aggregatedMetricRepository;
	private final AggregatedMetricMapper aggregatedMetricMapper;

	public AggregatedMetricServiceImpl(AggregatedMetricRepository aggregatedMetricRepository,
	                                   AggregatedMetricMapper aggregatedMetricMapper) {
		this.aggregatedMetricRepository = aggregatedMetricRepository;
		this.aggregatedMetricMapper = aggregatedMetricMapper;
	}

	@Override
	public AggregatedMetricDto createAggregatedMetric(AggregatedMetricDto dto) {
		AggregatedMetric entity = aggregatedMetricMapper.toEntity(dto);
		AggregatedMetric saved = aggregatedMetricRepository.save(entity);
		return aggregatedMetricMapper.toDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public AggregatedMetricDto getAggregatedMetricById(UUID id) {
		return aggregatedMetricRepository.findById(id)
				.map(aggregatedMetricMapper::toDTO)
				.orElseThrow(() -> new RuntimeException("AggregatedMetric not found with id: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AggregatedMetricDto> getMetricsByNameAndTimeRange(String metricName, OffsetDateTime start, OffsetDateTime end) {
		return aggregatedMetricRepository.findByMetricNameAndTimeWindowStartBetween(metricName, start, end)
				.stream()
				.map(aggregatedMetricMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<AggregatedMetricDto> getMetricsByRestaurantAndTimeRange(UUID restaurantId, OffsetDateTime start, OffsetDateTime end) {
		return aggregatedMetricRepository.findByRestaurantIdAndTimeWindowStartBetween(restaurantId, start, end)
				.stream()
				.map(aggregatedMetricMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public void deleteAggregatedMetricById(UUID id) {
		aggregatedMetricRepository.deleteById(id);
	}
}