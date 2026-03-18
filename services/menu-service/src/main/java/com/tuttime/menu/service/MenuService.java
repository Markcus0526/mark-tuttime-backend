package com.tuttime.menu.service;

import com.tuttime.menu.dto.MenuItemRequest;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.repository.MenuItemRepository;
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
