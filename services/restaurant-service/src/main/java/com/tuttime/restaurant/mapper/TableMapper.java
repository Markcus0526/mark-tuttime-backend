package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.DishTable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)// This is the magic line
public interface TableMapper {

	DishTable toEntity(TableDto.TableRequest request);

	@Mapping(target = "tableId", source = "id")
	@Mapping(target = "zoneId", source = "zone.id")
	TableDto.TableResponse toResponse(DishTable zone);

	void updateEntityFromRequest(TableDto.TableRequest request, @MappingTarget DishTable entity);
}

