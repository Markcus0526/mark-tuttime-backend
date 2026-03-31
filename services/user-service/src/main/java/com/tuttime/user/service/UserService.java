package com.tuttime.user.service;

import com.tuttime.user.dto.*;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface UserService {

	UserResponse createUser(UserCreateRequest request);

	UserResponse getUserById(UUID id);

	UserResponse updateUser(UUID id, UserUpdateRequest request);

	void softDeleteUser(UUID id);

	Page<UserResponse> listUsers(boolean includeDeleted, int page, int size, String sortBy, String direction);

	void assignRoleToUser(UUID userId, String roleName);

	void removeRoleFromUser(UUID userId, String roleName);

	void addAuthMethod(UUID userId, AuthMethodCreateRequest request);

	void verifyPhoneCode(String phoneNumber, String code);

	void requestPasswordReset(String emailOrPhone);

	void resetPassword(String token, String newPassword);

	void updateUserLocation(UUID userId, UserLocationRequest userLocationRequest);
}

