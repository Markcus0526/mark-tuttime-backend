package com.tuttime.user.repository;

import com.tuttime.user.entity.Role;
import com.tuttime.user.entity.UserRole;
import com.tuttime.user.entity.UserRoleId;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

	boolean existsByUserIdAndRole(UUID userId, Role role);

	void deleteByUserIdAndRole(UUID userId, Role role);

	Optional<UserRole> findByUserIdAndRole(UUID userId, Role role);

	Optional<UserRole> findByUserId(UUID userId);

	@Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.user.id = :userId")
	long countByUserId(@Param("userId") UUID userId);

}

