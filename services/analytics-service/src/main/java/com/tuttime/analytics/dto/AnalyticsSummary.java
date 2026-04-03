package com.tuttime.analytics.dto;

public record AnalyticsSummary(
		String category,
		long count,
		String lastUpdated
) {
}