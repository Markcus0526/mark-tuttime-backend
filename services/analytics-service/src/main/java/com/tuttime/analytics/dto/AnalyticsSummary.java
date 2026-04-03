package com.tuttime.analytics.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AnalyticsSummary(
		String category,
		long count,
		String lastUpdated
) {}