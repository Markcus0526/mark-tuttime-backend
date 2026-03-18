package com.tuttime.menu.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MenuItemRequest {
  private String name;
  private BigDecimal price;
  private Long categoryId;
  private List<Long> modifierIds;
}
