package com.tuttime.menu.grpc;

import com.tuttime.common.proto.*;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.repository.MenuCategoryRepository;
import com.tuttime.menu.repository.MenuItemRepository;
import com.tuttime.menu.repository.ModifierRepository;
import io.grpc.Status;
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
						.setCategory(category.getName())
						.setAvailable(menuItem.getAvailable());

				// 3. Map Modifiers (from MenuItem entity)
				if (menuItem.getModifierGroups() != null) {
					menuItem.getModifierGroups().forEach(group -> {
						ModifierGroup.Builder groupBuilder = ModifierGroup.newBuilder()
								.setGroupName(group.getName())
								.setMinSelect(group.getMinSelect())
								.setMaxSelect(group.getMaxSelect());

						group.getModifiers().forEach(opt -> groupBuilder.addOptions(
								ModifierOption.newBuilder()
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

	@Override
	@Transactional(readOnly = true)
	public void getMenuDetails(MenuDetailsRequest request, StreamObserver<MenuDetailsResponse> responseObserver) {
		try {
			// 1. Convert String IDs from gRPC to UUIDs
			List<UUID> uuids = request.getMenuItemIdsList().stream()
					.map(UUID::fromString)
					.toList();

			// 2. Fetch from DB (Use FetchType.EAGER or EntityGraph for ModifierGroups)
			List<MenuItem> entities = menuItemRepository.findAllById(uuids);

			// 3. Build the Response
			MenuDetailsResponse.Builder responseBuilder = MenuDetailsResponse.newBuilder();

			for (MenuItem entity : entities) {
				responseBuilder.addItems(mapToProto(entity));
			}

			responseObserver.onNext(responseBuilder.build());
			responseObserver.onCompleted();

		} catch (Exception e) {
			log.error("Error fetching menu details: ", e);
			responseObserver.onError(Status.INTERNAL
					.withDescription("Internal error fetching menu items")
					.asRuntimeException());
		}
	}

	private MenuItemDetail mapToProto(MenuItem entity) {
		MenuItemDetail.Builder builder = MenuItemDetail.newBuilder()
				.setMenuItemId(entity.getId().toString())
				.setName(entity.getName())
				.setDescription(entity.getDescription() != null ? entity.getDescription() : "")
				.setBasePrice(entity.getPrice().doubleValue()) // DB column is 'price'
				.setAvailable(entity.getAvailable()); // DB column is 'available'

		// Map through the join table: menu_item_modifier_groups
		if (entity.getModifierGroups() != null) {
			entity.getModifierGroups().forEach(group -> {
				ModifierGroup.Builder groupBuilder = ModifierGroup.newBuilder()
						.setGroupId(group.getId().toString())
						.setGroupName(group.getName())
						.setMinSelect(group.getMinSelect()) // DB column 'min_select'
						.setMaxSelect(group.getMaxSelect()); // DB column 'max_select'

				// Map the child choices from the 'modifiers' table
				if (group.getModifiers() != null) {
					group.getModifiers().forEach(modifier -> {
						groupBuilder.addOptions(ModifierOption.newBuilder()
								.setOptionId(modifier.getId().toString())
								.setName(modifier.getName())
								.setExtraPrice(modifier.getPriceAdjustment().doubleValue()) // DB column 'price_adjustment'
								.build());
					});
				}
				builder.addModifierGroups(groupBuilder.build());
			});
		}
		return builder.build();
	}

}
