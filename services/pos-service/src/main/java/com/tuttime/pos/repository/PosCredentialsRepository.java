package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PosCredentialsRepository extends JpaRepository<PosCredentials, Long> {
	Optional<PosCredentials> findByRestaurantIdAndPosSystem(Long restaurantId, PosCredentials.PosSystem posSystem);
}
