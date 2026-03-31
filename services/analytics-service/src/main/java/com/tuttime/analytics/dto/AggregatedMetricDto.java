package com.tuttime.analytics.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AggregatedMetricDto {

	private UUID id;
	private String metricName;
	private double value;
	private OffsetDateTime timeWindowStart;
	private OffsetDateTime timeWindowEnd;
	private UUID restaurantId;

}