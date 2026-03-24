package com.tuttime.menu.controller;

import com.tuttime.menu.dto.MenuCategoryDto;
import com.tuttime.menu.service.MenuCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/menu-categories")
@RequiredArgsConstructor
@Tag(name = "Menu Categories", description = "Operations related to menu categories")
public class MenuCategoryController {

  private final MenuCategoryService service;

  @PostMapping
  @Operation(summary = "Create a new menu category")
  public ResponseEntity<MenuCategoryDto> create(@RequestBody MenuCategoryDto dto) {
    return ResponseEntity.ok(service.create(dto));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get menu category by ID")
  public ResponseEntity<MenuCategoryDto> getById(@PathVariable Long id) {
    return ResponseEntity.ok(service.getById(id));
  }

  @GetMapping("/branch/{branchId}")
  @Operation(summary = "Get all menu categories by branch ID")
  public ResponseEntity<List<MenuCategoryDto>> getByBranch(@PathVariable Long branchId) {
    return ResponseEntity.ok(service.getByBranchId(branchId));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update menu category by ID")
  public ResponseEntity<MenuCategoryDto> update(@PathVariable Long id, @RequestBody MenuCategoryDto dto) {
    return ResponseEntity.ok(service.update(id, dto));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete menu category by ID")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}