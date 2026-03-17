package com.tuttime.menuservice.service;

import com.tuttime.menuservice.dto.MenuItemRequest;
import com.tuttime.menuservice.entity.MenuItem;
import com.tuttime.menuservice.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuService {

  private final MenuItemRepository menuItemRepository;

  public MenuItem createMenuItem(MenuItemRequest request) {
    MenuItem item = MenuItem.builder()
        .name(request.getName())
        .price(request.getPrice())
        .build();
    return menuItemRepository.save(item);
  }

}
