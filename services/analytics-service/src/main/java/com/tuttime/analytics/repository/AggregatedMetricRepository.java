package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.AggregatedMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AggregatedMetricRepository extends JpaRepository<AggregatedMetric, UUID> {
	List<AggregatedMetric> findByMetricNameAndTimeWindowStartBetween(String metricName, OffsetDateTime start, OffsetDateTime end);
	List<AggregatedMetric> findByRestaurantIdAndTimeWindowStartBetween(UUID restaurantId, OffsetDateTime start, OffsetDateTime end);
}