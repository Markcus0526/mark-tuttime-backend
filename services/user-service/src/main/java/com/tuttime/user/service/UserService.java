package com.tuttime.user.service;

import com.tuttime.common.enums.UserStatus;
import com.tuttime.user.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface UserService {

    UserDto.UserResponse createUser(UserDto.UserCreateRequest request);

    UserDto.UserResponse getUserById(UUID id);

    UserDto.UserResponse updateUser(UUID id, UserDto.UserUpdateRequest request);

    UserDto.UserResponse updateMe(UUID id, UserDto.UserUpdateRequest request, MultipartFile file);

    void deleteUser(UUID id);

    Page<UserDto.UserResponse> findUsers(String query, UserStatus status, String roleName, Pageable pageable);

    void assignRoleToUser(UUID userId, String roleName);

    void removeRoleFromUser(UUID userId, String roleName);

    void addAuthMethod(UUID userId, UserDto.AuthMethodCreateRequest request);

    void updateUserLocation(UUID userId, UserDto.UserLocationRequest userLocationRequest);

    UserDto.UserResponse blockUser(UUID id);

    UserDto.UserResponse unblockUser(UUID id);
}

