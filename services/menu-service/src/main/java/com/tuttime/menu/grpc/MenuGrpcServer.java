package com.tuttime.menu.grpc;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.proto.*;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.Modifier;
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
			UUID itemId = UUID.fromString(request.getMenuItemId());

			// 1. Fetch Base Item Price
			BigDecimal totalPrice = menuItemRepository.findById(itemId)
					.map(MenuItem::getPrice)
					.orElseThrow(() -> new InvalidRequestException("Menu item not found"));

			// 2. Fetch and Sum Modifier Adjustments
			if (!request.getModifierIdsList().isEmpty()) {
				BigDecimal modifiersTotal = request.getModifierIdsList().stream()
						.map(UUID::fromString)
						.map(id -> modifierRepository.findById(id)
								.map(Modifier::getPriceAdjustment)
								.orElse(BigDecimal.ZERO))
						.reduce(BigDecimal.ZERO, BigDecimal::add);

				totalPrice = totalPrice.add(modifiersTotal);
			}

			// 3. Build Response
			PriceResponse response = PriceResponse.newBuilder()
					.setVerifiedTotalPrice(totalPrice.toString())
					.build();

			responseObserver.onNext(response);
			responseObserver.onCompleted();
		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	@Override
	@Transactional
	public void getPreOrderMenu(MenuRequest request, StreamObserver<MenuResponse> responseObserver) {
		try {
			UUID branchId = UUID.fromString(request.getBranchId());

			// 1. Fetch categories for the branch (MenuCategoryRepository)
			List<MenuCategory> categories = menuCategoryRepository.findAllByBranchIdOrderByPriorityAsc(branchId);

			MenuResponse.Builder responseBuilder = MenuResponse.newBuilder();

			for (MenuCategory category : categories) {
				// 2. Access the MenuItemCategory relationships
				for (MenuItem menuItem : category.getItems()) {
					// Skip if the item is not available
					if (!menuItem.getAvailable()) continue;

					MenuItemDetailResponse.Builder itemBuilder = MenuItemDetailResponse.newBuilder()
							.setMenuItemId(menuItem.getId().toString())
							.setName(menuItem.getName())
							.setDescription(menuItem.getDescription() != null ? menuItem.getDescription() : "")
							.setBasePrice(menuItem.getPrice().toString())
							.setCategory(category.getName())
							.setAvailable(menuItem.getAvailable());

					// 3. Map Modifiers (from MenuItem entity)
					if (menuItem.getModifierGroups() != null) {
						menuItem.getModifierGroups().forEach(group -> {
							ModifierGroupResponse.Builder groupBuilder = ModifierGroupResponse.newBuilder()
									.setGroupName(group.getName())
									.setMinSelect(group.getMinSelect())
									.setMaxSelect(group.getMaxSelect());

							group.getModifiers().forEach(opt -> groupBuilder.addOptions(
									ModifierOptionResponse.newBuilder()
											.setName(opt.getName())
											.setExtraPrice(opt.getPriceAdjustment().toString())
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

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
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

		} catch (IllegalArgumentException e) {
			responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("<<< gRPC>>> Invalid argument: " + e.getMessage()).asRuntimeException());
		} catch (InvalidRequestException e) {
			// Map your custom business exception to NOT_FOUND instead of INTERNAL
			responseObserver.onError(Status.NOT_FOUND.withDescription("<<< gRPC>>> Invalid request: " + e.getMessage()).asRuntimeException());
		} catch (Exception e) {
			// Keep this for actual system failures (DB down, etc.)
			responseObserver.onError(Status.INTERNAL.withDescription("<<< gRPC>>> Unexpected error: " + e.getMessage()).asRuntimeException());
		}
	}

	private MenuItemDetailResponse mapToProto(MenuItem entity) {
		MenuItemDetailResponse.Builder builder = MenuItemDetailResponse.newBuilder()
				.setMenuItemId(entity.getId().toString())
				.setName(entity.getName())
				.setDescription(entity.getDescription() != null ? entity.getDescription() : "")
				.setBasePrice(entity.getPrice().toString()) // DB column is 'price'
				.setAvailable(entity.getAvailable()); // DB column is 'available'

		// Map through the join table: menu_item_modifier_groups
		if (entity.getModifierGroups() != null) {
			entity.getModifierGroups().forEach(group -> {
				ModifierGroupResponse.Builder groupBuilder = ModifierGroupResponse.newBuilder()
						.setGroupId(group.getId().toString())
						.setGroupName(group.getName())
						.setMinSelect(group.getMinSelect()) // DB column 'min_select'
						.setMaxSelect(group.getMaxSelect()); // DB column 'max_select'

				// Map the child choices from the 'modifiers' table
				if (group.getModifiers() != null) {
					group.getModifiers().forEach(modifier -> {
						groupBuilder.addOptions(ModifierOptionResponse.newBuilder()
								.setOptionId(modifier.getId().toString())
								.setName(modifier.getName())
								.setExtraPrice(modifier.getPriceAdjustment().toString()) // DB column 'price_adjustment'
								.build());
					});
				}
				builder.addModifierGroups(groupBuilder.build());
			});
		}
		return builder.build();
	}

}
