package com.tuttime.admin.service;

import com.tuttime.admin.dto.MiningFormulaConfigRequest;
import com.tuttime.admin.dto.MiningFormulaConfigResponse;

/**
 * Admin Mining Formula Service Interface
 */
public interface AdminMiningFormulaService {

	/**
	 * Gets the current mining formula configuration
	 *
	 * @return MiningFormulaConfigResponse containing current configuration
	 */
	MiningFormulaConfigResponse getMiningFormulaConfig();

	/**
	 * Updates the mining formula configuration
	 *
	 * @param request   The configuration update request
	 * @param updatedBy The admin ID who is updating the configuration
	 * @return MiningFormulaConfigResponse containing updated configuration
	 */
	MiningFormulaConfigResponse updateMiningFormulaConfig(MiningFormulaConfigRequest request, Long updatedBy);
}

