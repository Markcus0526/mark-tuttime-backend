package com.tuttime.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "aggregated_metrics", indexes = {
		@Index(name = "idx_aggregated_metrics_name_time", columnList = "metricName, timeWindowStart, timeWindowEnd"),
		@Index(name = "idx_aggregated_metrics_restaurant", columnList = "restaurantId")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatedMetric {

	@Id
	@GeneratedValue
	private UUID id;

	@Column(name = "metric_name", nullable = false, length = 100)
	private String metricName;

	@Column(name = "value", precision = 19, scale = 4) // Adjust precision/scale as needed
	private java.math.BigDecimal value;

	@Column(name = "time_window_start", nullable = false)
	private OffsetDateTime timeWindowStart;

	@Column(name = "time_window_end", nullable = false)
	private OffsetDateTime timeWindowEnd;

	@Column(name = "restaurant_id")
	private UUID restaurantId;
}
