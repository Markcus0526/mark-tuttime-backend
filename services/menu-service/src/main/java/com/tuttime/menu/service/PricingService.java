package com.tuttime.menu.service;

import com.tuttime.menu.entity.Discount;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.enums.DiscountType;
import com.tuttime.menu.repository.DiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public interface PricingService {

	BigDecimal calculateFinalPrice(MenuItem item);

	BigDecimal applyDiscount(BigDecimal price, Discount discount);
}