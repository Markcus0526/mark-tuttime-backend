package com.tuttime.order.service;

import com.tuttime.common.event.proto.ActiveDiscountDto;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderDiscountCalculator {
	private final MenuGrpcClient menuGrpcClient;

	public BigDecimal calculateFinalTotal(UUID branchId, List<OrderItem> items) {
		// Compute subtotal from items + modifiers
		BigDecimal subtotal = items.stream()
				.map(item -> {
					BigDecimal itemBase = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
					BigDecimal modifiersTotal = item.getModifiers().stream()
							.map(m -> m.getPriceAdjustment().multiply(BigDecimal.valueOf(item.getQuantity())))
							.reduce(BigDecimal.ZERO, BigDecimal::add);
					return itemBase.add(modifiersTotal);
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Fetch active order discounts (if any)
		List<ActiveDiscountDto> discounts = menuGrpcClient.getActiveOrderDiscounts(branchId);
		BigDecimal finalTotal = subtotal;

		for (ActiveDiscountDto discount : discounts) {
			if ("PERCENTAGE".equalsIgnoreCase(discount.getDiscountType())) {
				BigDecimal discountAmount = subtotal.multiply(BigDecimal.valueOf(discount.getAmount() / 100.0));
				finalTotal = finalTotal.subtract(discountAmount);
			} else if ("FIXED_AMOUNT".equalsIgnoreCase(discount.getDiscountType())) {
				finalTotal = finalTotal.subtract(BigDecimal.valueOf(discount.getAmount()));
			}
			if (finalTotal.compareTo(BigDecimal.ZERO) < 0) finalTotal = BigDecimal.ZERO;
		}
		return finalTotal;
	}
}
