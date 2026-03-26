package com.tuttime.analytics.service;

import com.tuttime.analytics.dto.AggregatedMetricDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AggregatedMetricService {
	AggregatedMetricDto createAggregatedMetric(AggregatedMetricDto dto);
	AggregatedMetricDto getAggregatedMetricById(UUID id);
	List<AggregatedMetricDto> getMetricsByNameAndTimeRange(String metricName, OffsetDateTime start, OffsetDateTime end);
	List<AggregatedMetricDto> getMetricsByRestaurantAndTimeRange(UUID restaurantId, OffsetDateTime start, OffsetDateTime end);
	void deleteAggregatedMetricById(UUID id);
}