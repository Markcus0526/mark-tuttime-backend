package com.tuttime.analytics.mapper;

import com.tuttime.analytics.dto.AdminDto;
import com.tuttime.analytics.entity.UserReliabilityIndex;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AdminMapper {

	@Mapping(target = "requiresFullPrepay", source = "reliabilityScore", qualifiedByName = "calculatePrepay")
	AdminDto.UserReliabilityResponse toResponse(UserReliabilityIndex entity);

	@Named("calculatePrepay")
	default boolean calculatePrepay(Integer score) {
		return score != null && score < 25;
	}
}