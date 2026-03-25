package com.tuttime.admin.repository;

import com.tuttime.admin.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * System Configuration Repository
 *
 * <p>Now includes mining formula configuration fields (merged from mining_formula_config table)
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

	/**
	 * Get system configuration (singleton - always ID 1)
	 */
	Optional<SystemConfig> findByConfigId(Long configId);

	/**
	 * Get the active system configuration (singleton - always ID 1, is_active = true)
	 *
	 * @param configId The configuration ID (should be 1)
	 * @return Optional containing the active configuration if it exists
	 */
	Optional<SystemConfig> findByConfigIdAndIsActiveTrue(Long configId);

	/**
	 * Get the system configuration singleton
	 */
	default Optional<SystemConfig> getSystemConfig() {
		return findByConfigId(1L);
	}
}

