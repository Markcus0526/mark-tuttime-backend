package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)// This is the magic line
public interface TableMapper {

	DishTable toEntity(TableDto.TableRequest request);

	TableDto.TableResponse toResponse(DishTable zone);

	void updateEntityFromRequest(TableDto.TableRequest request, @MappingTarget DishTable entity);
}

