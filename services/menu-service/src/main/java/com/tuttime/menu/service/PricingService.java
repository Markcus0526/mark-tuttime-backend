package com.tuttime.menu.service;

import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.entity.MenuItem;

import java.math.BigDecimal;

public interface PricingService {

	BigDecimal calculateFinalPrice(MenuItem item);

	BigDecimal applyDiscount(BigDecimal price, Discount discount);
}