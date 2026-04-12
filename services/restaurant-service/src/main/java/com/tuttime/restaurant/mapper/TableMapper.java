package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.TableRequest;
import com.tuttime.restaurant.dto.TableResponse;
import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)// This is the magic line
public interface TableMapper {

	DishTable toEntity(TableRequest request);

	TableResponse toResponse(DishTable zone);
}

