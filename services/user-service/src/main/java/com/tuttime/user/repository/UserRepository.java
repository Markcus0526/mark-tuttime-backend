package com.tuttime.user.repository;

import com.tuttime.user.entity.User;
import com.tuttime.user.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	@EntityGraph(attributePaths = {"roles", "roles.role"})
	Optional<User> findById(UUID id);

	Optional<User> findByPhoneNumberAndStatusIsNot(String phoneNumber, UserStatus status);

	Optional<User> findByEmailAndStatusIsNot(String email, UserStatus status);

	// Standard lookups
	Optional<User> findByEmail(String email);

	Optional<User> findByPhoneNumber(String phoneNumber);

	@Query("SELECT u FROM User u WHERE " +
			"(u.phoneNumber = :phone AND cast(:phone as string) IS NOT NULL) OR " +
			"(u.email = :email AND cast(:email as string) IS NOT NULL)")
	Optional<User> findByPhoneNumberOrEmail(@Param("phone") String phone, @Param("email") String email);


	// Standard lookup: only active, non-deleted users
	@Query("SELECT u FROM User u WHERE u.id = :id AND u.status = :status")
	Optional<User> findActiveById(UUID id, UserStatus status);

	Page<User> findAllByStatus(UserStatus status, Pageable pageable);

	@Query(value = "SELECT u FROM User u " +
			"LEFT JOIN FETCH u.roles ur " +
			"LEFT JOIN FETCH ur.role " +
			"WHERE (:status IS NULL OR u.status = :status)",
			countQuery = "SELECT COUNT(u) FROM User u WHERE (:status IS NULL OR u.status = :status)")
	Page<User> findAllByOptionalStatus(@Param("status") UserStatus status, Pageable pageable);

	@Query(value = "SELECT DISTINCT u FROM User u " +
			"LEFT JOIN FETCH u.roles ur " + // Changed from userRoles to roles
			"LEFT JOIN FETCH ur.role r " +
			"WHERE (cast(:status as string) IS NULL OR u.status = :status) " +
			"AND (cast(:roleName as string) IS NULL OR r.name = :roleName)",
			countQuery = "SELECT COUNT(DISTINCT u) FROM User u " +
					"LEFT JOIN u.roles ur " +
					"LEFT JOIN ur.role r " +
					"WHERE (cast(:status as string) IS NULL OR u.status = :status) " +
					"AND (cast(:roleName as string) IS NULL OR r.name = :roleName)")
	Page<User> findAllByOptionalStatusAndRole(
			@Param("status") UserStatus status,
			@Param("roleName") String roleName,
			Pageable pageable
	);
}


