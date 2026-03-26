package com.tuttime.analytics.mapper;

import com.tuttime.analytics.dto.AggregatedMetricDto;
import com.tuttime.analytics.entity.AggregatedMetric;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AggregatedMetricMapper {
	AggregatedMetricMapper INSTANCE = Mappers.getMapper(AggregatedMetricMapper.class);

	AggregatedMetricDto toDTO(AggregatedMetric entity);
	AggregatedMetric toEntity(AggregatedMetricDto dto);
}
