package com.tuttime.admin.repository;

import com.tuttime.admin.entity.MiningFormulaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Mining Formula Configuration
 *
 * @deprecated This repository is deprecated. The mining_formula_config table has been merged into system_config.
 * Use {@link SystemConfigRepository} and {@link com.tuttime.admin.service.AdminMiningFormulaService} instead.
 */
@Deprecated
@Repository
public interface MiningFormulaConfigRepository extends JpaRepository<MiningFormulaConfig, Long> {

	/**
	 * Gets the singleton mining formula configuration (config_id = 1)
	 *
	 * @return Optional containing the configuration if it exists
	 */
	Optional<MiningFormulaConfig> findByConfigId(Long configId);

	/**
	 * Gets the active mining formula configuration
	 *
	 * @return Optional containing the active configuration if it exists
	 */
	Optional<MiningFormulaConfig> findByConfigIdAndIsActiveTrue(Long configId);
}

