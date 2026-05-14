package com.tuttime.order.grpc.client;

import com.tuttime.common.event.grpc.GrpcExecutor;
import com.tuttime.common.event.proto.*;
import com.tuttime.order.entity.Order;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tuttime.common.constants.ServiceNames.POS_SERVICE;

@Service
@RequiredArgsConstructor
@Slf4j
public class PosGrpcClient {

	@Value("${grpc.client-deadline-seconds}")
	private int grpcDeadline;

	@GrpcClient("pos-service")
	private PosServiceGrpc.PosServiceBlockingStub posServiceBlockingStub;

	private final GrpcExecutor grpcExecutor;
	private final OrderService orderService;
	private final MenuGrpcClient menuGrpcClient;

	public boolean pushToKitchen(Order order) {
		// 1. Collect all unique Menu Item IDs from the order
		List<String> menuItemIds = order.getItems().stream()
				.map(item -> item.getMenuItemId().toString())
				.distinct()
				.toList();

		// 2. BULK FETCH: One network call to get all names/descriptions
		List<MenuItemDetailPto> menuDetailsResponse = menuGrpcClient.getMenuDetails(menuItemIds);

		// Convert to a Map for O(1) lookup speed during the loop
		Map<String, MenuItemDetailPto> detailMap = menuDetailsResponse.stream()
				.collect(Collectors.toMap(MenuItemDetailPto::getMenuItemId, d -> d));

		// 3. Build the Kitchen Request
		PushToKitchenRequest.Builder requestBuilder = PushToKitchenRequest.newBuilder()
				.setOrderId(order.getId().toString())
				.setBranchId(order.getBranchId().toString());

		order.getItems().forEach(item -> {
			MenuItemDetailPto detail = detailMap.get(item.getMenuItemId().toString());

			KitchenOrderItemDto.Builder itemEntry = KitchenOrderItemDto.newBuilder()
					.setMenuItemId(item.getMenuItemId().toString())
					.setName(detail != null ? detail.getName() : "Unknown Item") // HYDRATED NAME
					.setQuantity(item.getQuantity())
					.setPrice(item.getPrice().toString());

			// 4. Map Modifier Names
			item.getModifiers().forEach(mod -> {
				String modName = "Extra Option";
				if (detail != null) {
					// Flatten groups and options to find the name for this ID
					modName = detail.getModifierGroupsList().stream()
							.flatMap(g -> g.getOptionsList().stream())
							.filter(opt -> opt.getOptionId().equals(mod.getModifierId().toString()))
							.findFirst()
							.map(ModifierOptionPto::getName)
							.orElse("Extra Option");
				}
				itemEntry.addModifiers(modName); // HYDRATED MODIFIER NAME
			});

			requestBuilder.addItems(itemEntry.build());
		});

		return grpcExecutor.execute(POS_SERVICE, "pushToKitchen", () ->
				posServiceBlockingStub
						.withDeadlineAfter(grpcDeadline, TimeUnit.SECONDS)
						.pushToKitchen(requestBuilder.build()).getSuccess());
	}


//	private List<PosOrderItem> mapItemsToPos(List<OrderItem> items) {
//		return items.stream()
//				.map(item -> PosOrderItem.newBuilder()
//						.setMenuItemId(item.getMenuItemId().toString())
////						.setName(item.getName())
//						.setQuantity(item.getQuantity())
//						.setPrice(item.getPrice().doubleValue())
//						// Assuming your OrderItem entity has a collection of modifiers
//						.addAllModifiers(item.getModifiers().stream()
//								.map(OrderItemModifier::getModifierId)
//								.toList())
//						.build())
//				.toList();
//	}
}
