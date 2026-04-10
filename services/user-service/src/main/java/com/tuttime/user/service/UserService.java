package com.tuttime.user.service;

import com.tuttime.user.dto.*;
import com.tuttime.user.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

	UserResponse createUser(UserCreateRequest request);

	UserResponse getUserById(UUID id);

	UserResponse updateUser(UUID id, UserUpdateRequest request);

	void deleteUser(UUID id);

	Page<UserResponse> findUsers(String query, UserStatus status, String roleName, Pageable pageable);

	void assignRoleToUser(UUID userId, String roleName);

	void removeRoleFromUser(UUID userId, String roleName);

	void addAuthMethod(UUID userId, AuthMethodCreateRequest request);

	void updateUserLocation(UUID userId, UserLocationRequest userLocationRequest);

	UserResponse blockUser(UUID id);

	UserResponse unblockUser(UUID id);
}

