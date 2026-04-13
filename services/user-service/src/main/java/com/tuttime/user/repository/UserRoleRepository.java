package com.tuttime.user.repository;

import com.tuttime.user.entity.Role;
import com.tuttime.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

	boolean existsByUserIdAndRole(UUID userId, Role role);

	void deleteByUserIdAndRole(UUID userId, Role role);

	Optional<UserRole> findByUserIdAndRole(UUID userId, Role role);

	Optional<UserRole> findByUserId(UUID userId);

}

