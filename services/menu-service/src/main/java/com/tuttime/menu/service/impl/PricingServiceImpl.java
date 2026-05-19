package com.tuttime.menu.service.impl;

import com.tuttime.menu.dto.DiscountDto;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final DiscountRepository discountRepository;

    @Override
    public DiscountDto.PricingResult calculateFinalPrice(MenuItem item) {
        BigDecimal basePrice = item.getPrice();

        // Find discounts for the item or its parent category
        List<UUID> lookupIds = Arrays.asList(item.getId(), item.getCategory().getId());
        List<Discount> activeDiscounts = discountRepository.findActiveDiscounts(lookupIds);

        if (activeDiscounts.isEmpty()) {
            return new DiscountDto.PricingResult(basePrice, null);
        }

        // Strategy: Apply the largest discount found
        Discount bestDiscount = activeDiscounts.stream()
                .min(Comparator.comparing(d -> applyDiscount(basePrice, d)))
                .orElseThrow();

        return new DiscountDto.PricingResult(applyDiscount(basePrice, bestDiscount), bestDiscount.getName());
    }

    @Override
    public DiscountDto.PricingResult calculateFinalPrice(MenuItem item, List<Discount> preloadedDiscounts) {
        BigDecimal basePrice = item.getPrice();

        // Filter the preloaded list for this specific item or its category
        List<Discount> activeDiscounts = preloadedDiscounts.stream()
                .filter(d -> (d.getAppliesTo().name().equals("ITEM") && d.getReferenceId().equals(item.getId())) ||
                        (d.getAppliesTo().name().equals("CATEGORY") && d.getReferenceId().equals(item.getCategory().getId())))
                .toList();

        if (activeDiscounts.isEmpty()) {
            return new DiscountDto.PricingResult(basePrice, null);
        }

        Discount bestDiscount = activeDiscounts.stream()
                .min(Comparator.comparing(d -> applyDiscount(basePrice, d)))
                .orElseThrow();

        return new DiscountDto.PricingResult(applyDiscount(basePrice, bestDiscount), bestDiscount.getName());
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
