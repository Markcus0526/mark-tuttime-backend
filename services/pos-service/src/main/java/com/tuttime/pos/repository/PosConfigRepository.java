package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosConfig;
import com.tuttime.pos.entity.enums.PosProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosConfigRepository extends JpaRepository<PosConfig, Integer> {

	Optional<PosConfig> findByBranchId(UUID branchId);

	Optional<PosConfig> findByBranchIdAndEnabledTrue(UUID branchId);

	// Used to find if iiko or R-Keeper is configured
	Optional<PosConfig> findByPosProvider(PosProvider provider);

	// If you add restaurantId to the config table later:
	// Optional<PosSystemConfig> findByRestaurantIdAndEnabledTrue(UUID restaurantId);

	@Query(value = "SELECT * FROM pos_configs " +
			"WHERE posProvider = CAST(:posProvider AS VARCHAR) " +
			"AND enabled = true",
			nativeQuery = true)
	Optional<PosConfig> findByPosSystemAndEnabledTrue(@Param("posProvider") String posSystem);

	List<PosConfig> findAllByEnabledTrue();
}
