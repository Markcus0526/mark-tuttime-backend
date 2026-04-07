package com.tuttime.user.mapper;

import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserRole;
import com.tuttime.user.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

	UserResponse toResponse(User entity);

	// Add this helper method
	default String mapRoles(Set<UserRole> roles) {
		if (roles == null) {
			return null;
		}
		return roles.stream()
				.map(role -> role.getRole().getName()) // Change to .getRoleName() or .getName() based on your field
				.collect(Collectors.joining(", "));
	}
}