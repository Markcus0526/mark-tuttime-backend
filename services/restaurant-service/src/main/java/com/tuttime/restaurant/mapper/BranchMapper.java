package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Restaurant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // This is the magic line
public class BranchMapper {

    public BranchDto toDto(Branch entity) {
        if (entity == null) return null;
        BranchDto dto = new BranchDto();
        dto.setBranchId(entity.getBranchId());
        dto.setRestaurantId(entity.getRestaurant() != null ? entity.getRestaurant().getRestaurantId() : null);
        dto.setName(entity.getName());
        dto.setAddress(entity.getAddress());
        dto.setPhone(entity.getPhone());
        dto.setTimezone(entity.getTimezone());
        dto.setLocation(entity.getLocation());
        return dto;
    }

    public Branch toEntity(BranchDto dto, Restaurant restaurant) {
        if (dto == null) return null;
        Branch entity = new Branch();
        entity.setBranchId(dto.getBranchId());
        entity.setRestaurant(restaurant);
        entity.setName(dto.getName());
        entity.setAddress(dto.getAddress());
        entity.setPhone(dto.getPhone());
        entity.setTimezone(dto.getTimezone());
        entity.setLocation(dto.getLocation());
        return entity;
    }
}

