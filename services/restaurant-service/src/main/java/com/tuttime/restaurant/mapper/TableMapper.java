package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.Table;
import com.tuttime.restaurant.entity.Zone;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // This is the magic line
public class TableMapper {

    public TableDto toDto(Table entity) {
        if (entity == null) return null;
        TableDto dto = new TableDto();
        dto.setTableId(entity.getTableId());
        dto.setZoneId(entity.getZone() != null ? entity.getZone().getZoneId() : null);
        dto.setNumber(entity.getNumber());
        dto.setSeats(entity.getSeats());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    public Table toEntity(TableDto dto, Zone zone) {
        if (dto == null) return null;
        Table entity = new Table();
        entity.setTableId(dto.getTableId());
        entity.setZone(zone);
        entity.setNumber(dto.getNumber());
        entity.setSeats(dto.getSeats());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        return entity;
    }
}

