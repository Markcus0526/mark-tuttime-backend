package com.tuttime.user.mapper;

import com.tuttime.user.dto.UserDto;
import com.tuttime.user.entity.Role;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserRole;
import com.tuttime.user.repository.RoleRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected RoleRepository roleRepository;

    // Entity -> DTO
    @Mapping(target = "roles", qualifiedByName = "roleSetToStringSet")
    public abstract UserDto toDto(User user);

    // DTO -> Entity
    @Mapping(target = "roles", qualifiedByName = "stringSetToRoleSet")
    public abstract User toEntity(UserDto userDto);

    @Named("roleSetToStringSet")
    protected Set<String> roleSetToStringSet(Set<UserRole> userRoles) {
        if (userRoles == null) return null;
        return userRoles.stream()
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toSet());
    }

    @Named("stringSetToRoleSet")
    protected Set<UserRole> stringSetToRoleSet(Set<String> roleNames) {
        if (roleNames == null) return null;

        return roleNames.stream()
                .map(name -> {
                    Role role = roleRepository.findByName(name)
                            .orElseThrow(() -> new RuntimeException("Role not found: " + name));

                    // Create the Join Entity object
                    return UserRole.builder()
                            .role(role)
                            .roleId(role.getId())
                            .build();
                })
                .collect(Collectors.toSet());
    }

    // 3. Helper to satisfy "Can't map property Set<UserRole> to Set<String>"
    protected Set<String> mapRolesToStrings(Set<UserRole> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toSet());
    }
}
