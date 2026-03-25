package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.MiningFormulaConfigRequest;
import com.tuttime.admin.dto.MiningFormulaConfigResponse;
import com.tuttime.admin.entity.SystemConfig;
import com.tuttime.admin.repository.SystemConfigRepository;
import com.tuttime.admin.service.AdminMiningFormulaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Admin Mining Formula Service Implementation
 *
 * <p>Now uses SystemConfig entity instead of MiningFormulaConfig
 * (mining_formula_config table has been merged into system_config)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMiningFormulaServiceImpl implements AdminMiningFormulaService {

	private final SystemConfigRepository configRepository;
	private static final Long CONFIG_ID = 1L; // Singleton ID

	@Override
	@Transactional(readOnly = true)
	public MiningFormulaConfigResponse getMiningFormulaConfig() {
		log.debug("Getting mining formula configuration from system_config");

		SystemConfig config = configRepository.findByConfigIdAndIsActiveTrue(CONFIG_ID)
				.orElseGet(() -> {
					log.warn("No active system config found, returning default");
					return createDefaultConfig();
				});

		return mapToResponse(config);
	}

	@Override
	@Transactional
	public MiningFormulaConfigResponse updateMiningFormulaConfig(MiningFormulaConfigRequest request, Long updatedBy) {
		log.info("Updating mining formula configuration in system_config: updatedBy={}", updatedBy);

		SystemConfig config = configRepository.findByConfigId(CONFIG_ID)
				.orElseGet(() -> {
					log.info("Creating new system configuration with mining formula defaults");
					return SystemConfig.builder()
							.configId(CONFIG_ID)
							.build();
				});

		// Update fields if provided
		if (request.getBaseSpeedUnverified() != null) {
			config.setBaseSpeedUnverified(request.getBaseSpeedUnverified());
		}
		if (request.getBaseSpeedVerified() != null) {
			config.setBaseSpeedVerified(request.getBaseSpeedVerified());
		}
		if (request.getFriendBonusPerFriend() != null) {
			config.setFriendBonusPerFriend(request.getFriendBonusPerFriend());
		}
		if (request.getMaxFriends() != null) {
			config.setMaxFriends(request.getMaxFriends());
			// Recalculate max friend bonus
			config.setMaxFriendBonus(
					request.getFriendBonusPerFriend() != null
							? request.getFriendBonusPerFriend().multiply(BigDecimal.valueOf(request.getMaxFriends()))
							: config.getFriendBonusPerFriend().multiply(BigDecimal.valueOf(request.getMaxFriends()))
			);
		}
		if (request.getMaxFriendBonus() != null) {
			config.setMaxFriendBonus(request.getMaxFriendBonus());
		}
		if (request.getReferralBoostPerInvitation() != null) {
			config.setReferralBoostPerInvitation(request.getReferralBoostPerInvitation());
		}
		if (request.getBookingBonusPerBooking() != null) {
			config.setBookingBonusPerBooking(request.getBookingBonusPerBooking());
		}
		if (request.getBookingBoostDurationDays() != null) {
			config.setBookingBoostDurationDays(request.getBookingBoostDurationDays());
		}
		if (request.getMaxMiningSpeed() != null) {
			config.setMaxMiningSpeed(request.getMaxMiningSpeed());
		}
		if (request.getMinMiningIntervalMinutes() != null) {
			config.setMinMiningIntervalMinutes(request.getMinMiningIntervalMinutes());
		}
		if (request.getMaxMiningIntervalMinutes() != null) {
			config.setMaxMiningIntervalMinutes(request.getMaxMiningIntervalMinutes());
		}
		if (request.getMonthlyPoolAmount() != null) {
			config.setMonthlyPoolAmount(request.getMonthlyPoolAmount());
		}
		if (request.getDescription() != null) {
			config.setDescription(request.getDescription());
		}
		if (request.getIsActive() != null) {
			config.setIsActive(request.getIsActive());
		}

		config.setUpdatedBy(updatedBy);

		SystemConfig saved = configRepository.save(config);
		log.info("Mining formula configuration updated successfully in system_config: configId={}", saved.getConfigId());

		return mapToResponse(saved);
	}

	private SystemConfig createDefaultConfig() {
		return SystemConfig.builder()
				.configId(CONFIG_ID)
				.baseSpeedUnverified(new BigDecimal("0.3"))
				.baseSpeedVerified(new BigDecimal("1.0"))
				.friendBonusPerFriend(new BigDecimal("0.5"))
				.maxFriends(10)
				.maxFriendBonus(new BigDecimal("5.0"))
				.referralBoostPerInvitation(new BigDecimal("0.5"))
				.bookingBonusPerBooking(new BigDecimal("2.0"))
				.bookingBoostDurationDays(15)
				.maxMiningSpeed(new BigDecimal("77.777"))
				.minMiningIntervalMinutes(1)
				.maxMiningIntervalMinutes(5)
				.monthlyPoolAmount(new BigDecimal("50000000000.0"))
				.isActive(true)
				.build();
	}

	private MiningFormulaConfigResponse mapToResponse(SystemConfig config) {
		return MiningFormulaConfigResponse.builder()
				.configId(config.getConfigId())
				.baseSpeedUnverified(config.getBaseSpeedUnverified())
				.baseSpeedVerified(config.getBaseSpeedVerified())
				.friendBonusPerFriend(config.getFriendBonusPerFriend())
				.maxFriends(config.getMaxFriends())
				.maxFriendBonus(config.getMaxFriendBonus())
				.referralBoostPerInvitation(config.getReferralBoostPerInvitation())
				.bookingBonusPerBooking(config.getBookingBonusPerBooking())
				.bookingBoostDurationDays(config.getBookingBoostDurationDays())
				.maxMiningSpeed(config.getMaxMiningSpeed())
				.minMiningIntervalMinutes(config.getMinMiningIntervalMinutes())
				.maxMiningIntervalMinutes(config.getMaxMiningIntervalMinutes())
				.monthlyPoolAmount(config.getMonthlyPoolAmount())
				.description(config.getDescription())
				.isActive(config.getIsActive())
				.updatedBy(config.getUpdatedBy())
				.createdAt(config.getCreatedAt())
				.updatedAt(config.getUpdatedAt())
				.build();
	}
}

