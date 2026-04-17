package com.tuttime.order.grpc.client;

import com.tuttime.common.proto.PosOrderRequest;
import com.tuttime.common.proto.PosServiceGrpc;
import com.tuttime.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class PosGrpcClient {

	@GrpcClient("pos-service")
	private PosServiceGrpc.PosServiceBlockingStub posServiceBlockingStub;

	public void pushToKitchen(Order order) {
		// Map Order and MenuItems to POS-specific format
		PosOrderRequest request = PosOrderRequest.newBuilder()
				.setOrderId(order.getId().toString())
//				.setTableNumber(order.getTableNumber())
//				.addAllItems(mapItemsToPos(order.getItems()))
				.build();

		posServiceBlockingStub.pushToKitchen(request);
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
