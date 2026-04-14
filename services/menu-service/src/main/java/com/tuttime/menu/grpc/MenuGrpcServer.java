package com.tuttime.menu.grpc;

import com.tuttime.common.proto.*;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.ModifierRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class MenuGrpcServer extends MenuServiceGrpc.MenuServiceImplBase {

	private final MenuCategoryRepository menuCategoryRepository;
	private final MenuItemRepository menuItemRepository;
	private final ModifierRepository modifierRepository;

	@Override
	public void getPriceForOrder(PriceRequest request, StreamObserver<PriceResponse> responseObserver) {
		try {
			// 1. Fetch Base Item Price
			UUID itemId = UUID.fromString(request.getItemId());
			BigDecimal basePrice = menuItemRepository.findById(itemId)
					.map(item -> item.getPrice())
					.orElse(BigDecimal.ZERO);

			// 2. Calculate Modifiers Total
			double modifiersTotal = request.getModifierIdsList().stream()
					.map(UUID::fromString)
					.map(modId -> modifierRepository.findById(modId)
							.map(m -> m.getPriceAdjustment())
							.orElse(BigDecimal.ZERO))
					.reduce(BigDecimal.ZERO, BigDecimal::add)
					.doubleValue();

			// 3. Build and Send Response
			PriceResponse response = PriceResponse.newBuilder()
					.setBasePrice(basePrice.doubleValue())
					.setModifiersTotal(modifiersTotal)
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();

		} catch (Exception e) {
			// Handle parsing errors or missing data
			responseObserver.onError(io.grpc.Status.INTERNAL
					.withDescription("Error calculating price: " + e.getMessage())
					.asRuntimeException());
		}
	}

	@Override
	@Transactional
	public void getPreOrderMenu(MenuRequest request, StreamObserver<MenuResponse> responseObserver) {
		UUID branchId = UUID.fromString(request.getBranchId());

		// 1. Fetch categories for the branch (MenuCategoryRepository)
		List<MenuCategory> categories = menuCategoryRepository.findAllByBranchIdOrderByPriorityAsc(branchId);

		MenuResponse.Builder responseBuilder = MenuResponse.newBuilder();

		for (MenuCategory category : categories) {
			// 2. Access the MenuItemCategory relationships
			for (MenuItem menuItem : category.getItems()) {
				// Skip if the item is not available
				if (!menuItem.getAvailable()) continue;

				MenuItemDetail.Builder itemBuilder = MenuItemDetail.newBuilder()
						.setMenuItemId(menuItem.getId().toString())
						.setName(menuItem.getName())
						.setDescription(menuItem.getDescription() != null ? menuItem.getDescription() : "")
						.setBasePrice(menuItem.getPrice().doubleValue())
						.setCategory(category.getName());

				// 3. Map Modifiers (from MenuItem entity)
				if (menuItem.getModifierGroups() != null) {
					menuItem.getModifierGroups().forEach(group -> {
						ModifierGroup.Builder groupBuilder = ModifierGroup.newBuilder()
								.setGroupName(group.getName());

						group.getModifiers().forEach(opt -> groupBuilder.addOptions(
								Modifier.newBuilder()
										.setName(opt.getName())
										.setExtraPrice(opt.getPriceAdjustment().doubleValue())
										.build()
						));
						itemBuilder.addModifierGroups(groupBuilder);
					});
				}

				responseBuilder.addItems(itemBuilder);
			}
		}

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}
}
