package com.tuttime.user.mapper;

import com.tuttime.user.dto.AuthDto;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

	@Mapping(target = "userId", source = "user.id")
	@Mapping(target = "accessToken", source = "accessToken")
	@Mapping(target = "refreshToken", source = "refreshToken")
	@Mapping(target = "tokenType", constant = "Bearer")
	@Mapping(target = "expiresIn", source = "expiry")
	@Mapping(target = "roles", source = "user.roles", qualifiedByName = "mapRoles")
	AuthDto.AuthResponse toAuthResponse(
			User user,
			String accessToken,
			String refreshToken,
			long expiry,
			AuthDto.AuthResponse.RestaurantStaffDto staff);

	@Named("mapRoles")
	default List<String> mapRoles(java.util.Set<UserRole> roles) {
		if (roles == null) return List.of();
		return roles.stream()
				.map(ur -> ur.getRole().getName())
				.collect(Collectors.toList());
	}
}
