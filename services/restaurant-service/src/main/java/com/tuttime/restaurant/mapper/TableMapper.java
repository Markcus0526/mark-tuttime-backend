package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.enums.TableStatus;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)// This is the magic line
public interface TableMapper {

	Table toEntity(TableDto dto);

}

