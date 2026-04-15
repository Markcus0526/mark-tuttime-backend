package com.tuttime.menu.service.impl;

import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.enums.DiscountType;
import com.tuttime.menu.repository.DiscountRepository;
import com.tuttime.menu.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

	private final DiscountRepository discountRepository;

	@Override
	public BigDecimal calculateFinalPrice(MenuItem item) {
		BigDecimal basePrice = item.getPrice();

		// Find discounts for the item or its parent category
		List<UUID> lookupIds = Arrays.asList(item.getId(), item.getCategory().getId());
		List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(lookupIds);

		if (activeDiscounts.isEmpty()) {
			return basePrice;
		}

		// Strategy: Apply the largest discount found
		return activeDiscounts.stream()
				.map(d -> applyDiscount(basePrice, d))
				.min(BigDecimal::compareTo) // Smallest result = Best discount for customer
				.orElse(basePrice);
	}

	@Override
	public BigDecimal applyDiscount(BigDecimal price, Discount discount) {
		if (discount.getDiscountType() == DiscountType.PERCENTAGE) {
			// Price - (Price * percentage / 100)
			BigDecimal discountAmount = price.multiply(discount.getAmount())
					.divide(new BigDecimal("100"), RoundingMode.HALF_UP);
			return price.subtract(discountAmount);
		} else {
			// Price - flat amount
			return price.subtract(discount.getAmount()).max(BigDecimal.ZERO);
		}
	}
}
