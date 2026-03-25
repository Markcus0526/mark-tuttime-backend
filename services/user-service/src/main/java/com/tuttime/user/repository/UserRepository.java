package com.tuttime.user.repository;

import com.tuttime.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByPhoneNumber(String phoneNumber);

	Optional<User> findByEmail(String email);
}


