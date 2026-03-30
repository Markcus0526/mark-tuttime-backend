package com.tuttime.user.service.impl;

import com.tuttime.user.entity.Role;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserRole;
import com.tuttime.user.repository.RoleRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.repository.UserRoleRepository;
import com.tuttime.user.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserRoleServiceImpl implements UserRoleService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;

	@Override
	public void assignRole(UUID userId, String roleName) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new IllegalArgumentException("Role not found"));

		if (userRoleRepository.existsByUserIdAndRole(user.getId(), role)) {
			return; // Role already assigned
		}

		var userRole = new UserRole();
		userRole.setUserId(user.getId());
		userRole.setRole(role);
		userRoleRepository.save(userRole);
	}

	@Override
	public void removeRole(UUID userId, String roleName) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		Role role = roleRepository.findByName(roleName)
				.orElseThrow(() -> new IllegalArgumentException("Role not found"));

		userRoleRepository.deleteByUserIdAndRole(user.getId(), role);
	}
}
