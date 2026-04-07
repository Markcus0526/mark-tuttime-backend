package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.BranchRequest;
import com.tuttime.restaurant.dto.BranchResponse;
import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BranchMapper {

	Branch toEntity(BranchRequest request);

	@Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
	@Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
	BranchResponse toResponse(Branch entity);

	@Mapping(target = "location", ignore = true)
	void updateEntityFromRequest(BranchRequest request, @MappingTarget Branch entity);

}