package com.tuttime.analytics.service;

import com.tuttime.analytics.dto.EventResponse;
import com.tuttime.analytics.dto.HourlyBookingProjection;
import com.tuttime.analytics.dto.TopRestaurantProjection;
import com.tuttime.analytics.entity.SystemEvent;

import java.util.List;
import java.util.UUID;

public interface AnalyticsService {

	/**
	 * Internal logic used by Kafka Consumers to persist incoming events.
	 */
	void logEvent(String service, String type, UUID entityId, Object payload);

	/**
	 * Used by AnalyticsController to show a restaurant's activity feed.
	 */
	List<EventResponse> getRestaurantHistory(UUID restaurantId);

	/**
	 * Used by AnalyticsReportController for the Top 10 dashboard.
	 */
	List<TopRestaurantProjection> getTopRestaurants(int days);

	/**
	 * Used by AnalyticsReportController for the hourly distribution chart.
	 */
	List<HourlyBookingProjection> getBookingHeatmap(int days);

}