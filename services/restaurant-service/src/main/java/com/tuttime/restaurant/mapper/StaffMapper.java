package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.StaffDto;
import com.tuttime.restaurant.entity.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StaffMapper {

	/**
	 * Converts Entity to Response DTO.
	 * Maps nested 'id' fields from Restaurant and Branch objects to flat UUIDs.
	 */
	@Mapping(source = "id", target = "staffId")
	@Mapping(target = "restaurantId", source = "restaurant.id")
	@Mapping(target = "branchId", source = "branch.id")
	StaffDto.StaffResponse toResponse(Staff staff);

	/**
	 * Converts Request DTO to Entity.
	 * Fields like 'restaurant' and 'branch' are ignored here
	 * because they must be fetched from the DB and set in the Service layer.
	 */
	@Mapping(target = "restaurant", ignore = true)
	@Mapping(target = "branch", ignore = true)
	Staff toEntity(StaffDto.StaffRequest request);

	void updateEntityFromRequest(StaffDto.StaffUpdateRequest request, @MappingTarget Staff entity);
}

