package com.tuttime.user.repository;

import com.tuttime.user.entity.User;
import com.tuttime.user.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	Optional<User> findByUserIdAndDeletedFalse(UUID id);

	Optional<User> findByPhoneNumberAndDeletedFalse(String phoneNumber);

	Optional<User> findByEmailAndDeletedFalse(String email);

	@Query("SELECT u FROM User u WHERE " +
			"(cast(:phone as string) IS NOT NULL AND u.phoneNumber = :phone) OR " +
			"(cast(:email as string) IS NOT NULL AND u.email = :email)")
	Optional<User> findByPhoneNumberOrEmail(@Param("phone") String phone, @Param("email") String email);

	boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

	boolean existsByEmailAndDeletedFalse(String email);

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

	List<User> findAllByUserIdInAndDeletedFalse(Collection<UUID> userIds);
}


