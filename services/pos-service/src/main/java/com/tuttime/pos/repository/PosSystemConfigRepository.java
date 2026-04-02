package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosSystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PosSystemConfigRepository extends JpaRepository<PosSystemConfig, Integer> {

	// Used to find if iiko or R-Keeper is configured
	Optional<PosSystemConfig> findByPosSystem(String posSystem);

	// If you add restaurantId to the config table later:
	// Optional<PosSystemConfig> findByRestaurantIdAndEnabledTrue(UUID restaurantId);

	@Query(value = "SELECT * FROM pos_system_configs " +
			"WHERE pos_system = CAST(:posSystem AS VARCHAR) " +
			"AND enabled = true",
			nativeQuery = true)
	Optional<PosSystemConfig> findByPosSystemAndEnabledTrue(@Param("posSystem") String posSystem);
}
