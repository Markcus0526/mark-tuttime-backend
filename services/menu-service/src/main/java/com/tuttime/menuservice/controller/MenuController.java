package com.tuttime.menuservice.controller;

import com.tuttime.menuservice.dto.MenuItemRequest;
import com.tuttime.menuservice.entity.MenuItem;
import com.tuttime.menuservice.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController {

  private final MenuService menuService;

  @PostMapping("/items")
  public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItemRequest request) {
    return ResponseEntity.ok(menuService.createMenuItem(request));
  }

}
