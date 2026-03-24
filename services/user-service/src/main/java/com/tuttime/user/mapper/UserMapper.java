package com.tuttime.user.mapper;

import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserRole;
import com.tuttime.user.repository.RoleRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    protected RoleRepository roleRepository;

    public abstract UserDto toDto(User user);

    @Mapping(target = "roles", qualifiedByName = "stringToRoleSet")
    public abstract User toEntity(UserDto userDto);

    // 1. Logic to convert a Set of Strings to a Set of UserRole entities
    @Named("stringToRoleSet")
    protected Set<UserRole> stringToRoleSet(Set<String> roleNames) {
        if (roleNames == null) return null;

        return roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new RuntimeException("Role not found: " + name)))
                .collect(Collectors.toSet());
    }

    // 2. Logic to convert UserRole entities back to Strings for the DTO
    protected Set<String> rolesToNames(Set<UserRole> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());
    }

}

