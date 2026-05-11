package com.tuttime.user.repository;

import com.tuttime.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

	Optional<User> findByIdAndDeletedFalse(UUID id);

	Optional<User> findByPhoneNumberAndDeletedFalse(String phoneNumber);

	Optional<User> findByEmailAndDeletedFalse(String email);

	@Query("SELECT u FROM User u WHERE " +
			"(cast(:phone as string) IS NOT NULL AND u.phoneNumber = :phone) OR " +
			"(cast(:email as string) IS NOT NULL AND u.email = :email)")
	Optional<User> findByPhoneNumberOrEmail(@Param("phone") String phone, @Param("email") String email);

	boolean existsByPhoneNumberAndDeletedFalse(String phoneNumber);

	boolean existsByEmailAndDeletedFalse(String email);

	@Query("SELECT u FROM User u " +
			"LEFT JOIN FETCH u.roles r " +
			"LEFT JOIN FETCH r.role " +
			"WHERE u.id = :id AND u.deleted = false")
	Optional<User> findByIdWithRoles(UUID id);

	@Query("SELECT u FROM User u " +
			"LEFT JOIN FETCH u.roles r " +
			"LEFT JOIN FETCH r.role " +
			"WHERE u.id IN :ids AND u.deleted = false")
	List<User> findAllByIdInWithRoles(List<UUID> ids);
}


