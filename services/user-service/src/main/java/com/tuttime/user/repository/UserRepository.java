package com.tuttime.user.repository;

import com.tuttime.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByPhoneNumberAndIsDeletedFalse(String phoneNumber);

	Optional<User> findByEmailAndIsDeletedFalse(String email);

	List<User> findAllByIsDeletedFalse();

	Optional<User> findByIdAndIsDeletedFalse(UUID id);

	Optional<User> findByEmail(String email);

	Optional<User> findByPhoneNumber(String phoneNumber);

	Optional<User> findByPhoneNumberOrEmail(String phoneNumber, String email);

	Page<User> findByIsDeletedFalse(Pageable pageable);

	// Standard lookup: only active, non-deleted users
	@Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
	Optional<User> findActiveById(UUID id);

	// Used for the 'listUsers' method where 'includeDeleted' might be true
	Page<User> findAllByIsDeleted(boolean isDeleted, Pageable pageable);
}


