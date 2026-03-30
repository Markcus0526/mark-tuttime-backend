package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.enums.TableStatus;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)// This is the magic line
public class TableMapper {

	public Table toEntity(TableDto dto, Zone zone) {
		if (dto == null) return null;
		Table entity = new Table();
		entity.setTableId(dto.getTableId());
		entity.setZoneId(zone.getZoneId());
		entity.setNumber(dto.getNumber());
		entity.setSeats(dto.getSeats());
		entity.setStatus(dto.getStatus() != null ? dto.getStatus() : TableStatus.ACTIVE);
		return entity;
	}
}

