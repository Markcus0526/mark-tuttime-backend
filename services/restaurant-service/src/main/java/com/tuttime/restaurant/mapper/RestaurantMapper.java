package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.Restaurant;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // This is the magic line
public class RestaurantMapper {

	public RestaurantDto toDto(Restaurant entity) {
		if (entity == null)
			return null;
		return RestaurantDto.builder()
				.restaurantId(entity.getRestaurantId())
				.ownerUserId(entity.getOwnerUserId())
				.name(entity.getName())
				.description(entity.getDescription())
				.address(entity.getAddress())
				.location(entity.getLocation())
				.phone(entity.getPhone())
				.website(entity.getWebsite())
				.rating(entity.getRating())
				.build();
	}

	public Restaurant toEntity(RestaurantDto dto) {
		if (dto == null)
			return null;
		return Restaurant.builder()
				.restaurantId(dto.getRestaurantId())
				.ownerUserId(dto.getOwnerUserId())
				.name(dto.getName())
				.description(dto.getDescription())
				.address(dto.getAddress())
				.location(dto.getLocation())
				.phone(dto.getPhone())
				.website(dto.getWebsite())
				.rating(dto.getRating())
				.build();
	}
}
