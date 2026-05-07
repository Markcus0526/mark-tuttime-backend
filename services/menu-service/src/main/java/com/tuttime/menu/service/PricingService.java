package com.tuttime.menu.service;

import com.tuttime.menu.dto.DiscountDto;
import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.entity.MenuItem;

import java.math.BigDecimal;
import java.util.List;

public interface PricingService {

	DiscountDto.PricingResult calculateFinalPrice(MenuItem item);

	DiscountDto.PricingResult calculateFinalPrice(MenuItem item, List<Discount> preloadedDiscounts);

	BigDecimal applyDiscount(BigDecimal price, Discount discount);
}