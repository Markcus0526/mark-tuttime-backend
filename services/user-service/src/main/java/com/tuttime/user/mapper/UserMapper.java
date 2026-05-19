package com.tuttime.user.mapper;

import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(source = "id", target = "userId")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    UserDto.UserResponse toResponse(User entity);

    @Named("mapRoles")
    default String mapRoles(Set<UserRole> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.joining(", "));
    }
}