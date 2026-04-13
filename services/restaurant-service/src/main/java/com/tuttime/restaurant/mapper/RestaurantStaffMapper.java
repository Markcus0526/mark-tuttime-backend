package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.StaffDto;
import com.tuttime.restaurant.entity.RestaurantStaff;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantStaffMapper {

	/**
	 * Converts Entity to Response DTO.
	 * Maps nested 'id' fields from Restaurant and Branch objects to flat UUIDs.
	 */
	StaffDto.StaffResponse toResponse(RestaurantStaff staff);

	/**
	 * Converts Request DTO to Entity.
	 * Fields like 'restaurant' and 'branch' are ignored here
	 * because they must be fetched from the DB and set in the Service layer.
	 */
	RestaurantStaff toEntity(StaffDto.StaffRequest request);

	void updateEntityFromRequest(StaffDto.StaffUpdateRequest request, @MappingTarget RestaurantStaff entity);
}

