package com.tuttime.user.repository;

import com.tuttime.user.entity.UserLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserLocationRepository extends JpaRepository<UserLocation, UUID> {

	// Add custom queries if needed, e.g., find all locations for user
}
