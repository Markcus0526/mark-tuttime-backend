package com.tuttime.pos.service.impl;

import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.PosEvent;
import com.tuttime.pos.dto.liko.IikoProduct;
import com.tuttime.pos.dto.rkeeper.RK7MenuItem;
import com.tuttime.pos.entity.PosMenuMapping;
import com.tuttime.pos.repository.PosMenuMappingRepository;
import com.tuttime.pos.service.PosMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosMappingServiceImpl implements PosMappingService {

	private final PosMenuMappingRepository posMenuMappingRepository;
	private final EventProducer eventProducer;

	@Override
	@Transactional
	public void updateIikoMappings(UUID branchId, List<IikoProduct> products) {
		OffsetDateTime syncStartTime = OffsetDateTime.now();
		List<UUID> reactivatedIds = new ArrayList<>();

		log.info("Starting iiko menu sync for branch: {}", branchId);

		for (IikoProduct product : products) {
			PosMenuMapping mapping = posMenuMappingRepository
					.findByBranchIdAndExternalPosId(branchId, product.getId())
					.orElseGet(() -> {
						PosMenuMapping m = new PosMenuMapping();
						m.setBranchId(branchId);
						m.setExternalPosId(product.getId());
						return m;
					});

			BigDecimal newPrice = BigDecimal.valueOf(product.getPrice());

			// 1. PRICE UPDATE LOGIC
			// If already linked to TutTime and the price has changed in iiko
			if (mapping.getMenuItemId() != null && mapping.getExternalPrice() != null
					&& mapping.getExternalPrice().compareTo(newPrice) != 0) {

				log.info("Price change detected for item {}: {} -> {}",
						mapping.getMenuItemId(), mapping.getExternalPrice(), newPrice);

				PosEvent.PosPriceUpdatedEvent event = PosEvent.PosPriceUpdatedEvent.builder()
						.branchId(branchId)
						.menuItemId(mapping.getMenuItemId())
						.newPrice(newPrice)
						.build();
				eventProducer.sendPosPriceUpdatedEvent(event);
			}

			// 2. RE-ACTIVATION LOGIC
			if (mapping.getMenuItemId() != null &&
					(mapping.getLastSyncedAt() == null || mapping.getLastSyncedAt().isBefore(syncStartTime))) {
				reactivatedIds.add(mapping.getMenuItemId());
			}

			// Update local mapping data
			mapping.setExternalName(product.getName());
			mapping.setExternalPrice(newPrice);
			mapping.setLastSyncedAt(syncStartTime);
			posMenuMappingRepository.save(mapping);
		}

		// 3. Notify Menu Service for Activations
		if (!reactivatedIds.isEmpty()) {
			PosEvent.PosMenuUpdatedEvent event = PosEvent.PosMenuUpdatedEvent.builder()
					.branchId(branchId)
					.itemIds(reactivatedIds)
					.updateType("ACTIVATION")
					.build();
			eventProducer.sendPosMenuUpdatedEvent(event);
		}

		// 4. Handle DEACTIVATION (Sweep)
		List<UUID> missingItemIds = posMenuMappingRepository
				.findMenuItemIdsByBranchAndLastSyncedBefore(branchId, syncStartTime);

		if (!missingItemIds.isEmpty()) {
			posMenuMappingRepository.deactivateOldMappings(branchId, syncStartTime);

			PosEvent.PosMenuUpdatedEvent event = PosEvent.PosMenuUpdatedEvent.builder()
					.branchId(branchId)
					.itemIds(missingItemIds)
					.updateType("DEACTIVATION")
					.build();
			eventProducer.sendPosMenuUpdatedEvent(event);
		}
	}


	/**
	 * Specifically for R-Keeper nomenclature updates
	 */
	@Override
	@Transactional
	public void updateRKeeperMappings(UUID branchId, List<RK7MenuItem> rkItems) {
		// 1. Record the exact start time to use as a "heartbeat" marker
		OffsetDateTime syncStartTime = OffsetDateTime.now();
		List<UUID> reactivatedIds = new ArrayList<>();
		log.info("Starting R-Keeper menu sync for branch {} at {}", branchId, syncStartTime);

		for (RK7MenuItem rkItem : rkItems) {
			PosMenuMapping mapping = posMenuMappingRepository
					.findByBranchIdAndExternalPosId(branchId, rkItem.getIdent())
					.orElseGet(() -> {
						PosMenuMapping newMapping = new PosMenuMapping();
						newMapping.setBranchId(branchId);
						newMapping.setExternalPosId(rkItem.getIdent());
						return newMapping;
					});

			// 2. PRICE UPDATE LOGIC
			// Convert RKeeper price to BigDecimal and compare with stored external price
			if (rkItem.getPrice() != null) {
				BigDecimal newPrice = BigDecimal.valueOf(rkItem.getPrice());

				if (mapping.getMenuItemId() != null && mapping.getExternalPrice() != null
						&& mapping.getExternalPrice().compareTo(newPrice) != 0) {

					log.info("Price change detected for R-Keeper item {}: {} -> {}",
							mapping.getMenuItemId(), mapping.getExternalPrice(), newPrice);

					PosEvent.PosPriceUpdatedEvent event = PosEvent.PosPriceUpdatedEvent.builder()
							.branchId(branchId)
							.menuItemId(mapping.getMenuItemId())
							.newPrice(newPrice)
							.build();
					eventProducer.sendPosPriceUpdatedEvent(event);
				}
				mapping.setExternalPrice(newPrice);
			}

			// 3. RE-ACTIVATION LOGIC
			if (mapping.getMenuItemId() != null &&
					(mapping.getLastSyncedAt() == null || mapping.getLastSyncedAt().isBefore(syncStartTime))) {
				reactivatedIds.add(mapping.getMenuItemId());
			}

			mapping.setExternalName(rkItem.getName());

			// 4. Use the exact syncStartTime to avoid "drifting"
			mapping.setLastSyncedAt(syncStartTime);
			posMenuMappingRepository.save(mapping);
		}

		// 5. Notify Menu Service to SHOW items that reappeared
		if (!reactivatedIds.isEmpty()) {
			PosEvent.PosMenuUpdatedEvent event = PosEvent.PosMenuUpdatedEvent.builder()
					.branchId(branchId)
					.itemIds(reactivatedIds)
					.updateType("ACTIVATION")
					.build();
			eventProducer.sendPosMenuUpdatedEvent(event);
		}

		// 6. DEACTIVATION (Sweep): Find items that didn't receive the "heartbeat"
		List<UUID> missingItemIds = posMenuMappingRepository
				.findMenuItemIdsByBranchAndLastSyncedBefore(branchId, syncStartTime);

		if (!missingItemIds.isEmpty()) {
			log.warn("Detected {} items removed from R-Keeper for branch {}", missingItemIds.size(), branchId);
			posMenuMappingRepository.deactivateOldMappings(branchId, syncStartTime);

			PosEvent.PosMenuUpdatedEvent event = PosEvent.PosMenuUpdatedEvent.builder()
					.branchId(branchId)
					.itemIds(missingItemIds)
					.updateType("DEACTIVATION")
					.build();
			eventProducer.sendPosMenuUpdatedEvent(event);
		}
	}

	/**
	 * Resolves an internal TutTime UUID to an external POS String ID.
	 *
	 * @param branchId   The specific restaurant branch
	 * @param menuItemId The TutTime internal UUID
	 * @return The iiko GUID or R-Keeper Ident
	 */
	public String getExternalId(UUID branchId, String menuItemId) {
		return posMenuMappingRepository
				.findExternalIdByMenuItemId(branchId, UUID.fromString(menuItemId))
				.orElseThrow(() -> new RuntimeException("Mapping not found for item: " + menuItemId));
	}
}
