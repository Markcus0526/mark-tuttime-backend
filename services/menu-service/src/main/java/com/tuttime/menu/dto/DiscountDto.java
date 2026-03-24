package com.tuttime.menu.dto;

import com.tuttime.menu.entity.Discount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiscountDto {
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotNull(message = "Discount type is required")
    private Discount.DiscountTypeEnum discountType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "AppliesTo is required")
    private Discount.AppliesToEnum appliesTo;

    private Long referenceId;

    private OffsetDateTime startDate;

    private OffsetDateTime endDate;

    private Boolean isActive;

}