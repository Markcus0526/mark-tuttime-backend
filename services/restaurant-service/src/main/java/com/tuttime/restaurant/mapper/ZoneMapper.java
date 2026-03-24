package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // This is the magic line
public class ZoneMapper {

    public ZoneDto toDto(Zone entity) {
        if (entity == null) return null;
        ZoneDto dto = new ZoneDto();
        dto.setZoneId(entity.getZoneId());
        dto.setBranchId(entity.getBranch() != null ? entity.getBranch().getBranchId() : null);
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCapacity(entity.getCapacity());
        return dto;
    }

    public Zone toEntity(ZoneDto dto, Branch branch) {
        if (dto == null) return null;
        Zone entity = new Zone();
        entity.setZoneId(dto.getZoneId());
        entity.setBranch(branch);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCapacity(dto.getCapacity());
        return entity;
    }
}

