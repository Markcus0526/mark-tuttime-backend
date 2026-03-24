package com.tuttime.menu.controller;

import com.tuttime.menu.dto.StopListDto;
import com.tuttime.menu.service.StopListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stoplists")
@RequiredArgsConstructor
@Tag(name = "Stop Lists", description = "Operations related to stop lists")
public class StopListController {
    private final StopListService stopListService;

    @PostMapping
    @Operation(summary = "Create a new stop list")
    public ResponseEntity<StopListDto> create(@RequestBody StopListDto stopList) {
        return ResponseEntity.ok(stopListService.create(stopList));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get stop list by ID")
    public ResponseEntity<StopListDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stopListService.getById(id));
    }

    @GetMapping("/menuitem/{menuItemId}")
    @Operation(summary = "Get discounts by menu item ID")
    public ResponseEntity<List<StopListDto>> getByMenuitemId(@PathVariable Long menuItemId) {
        return ResponseEntity.ok(stopListService.getByMenuitemId(menuItemId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update stop list by ID")
    public ResponseEntity<StopListDto> update(@PathVariable Long id, @RequestBody StopListDto stopList) {
        return ResponseEntity.ok(stopListService.update(id, stopList));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete stop list by ID")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stopListService.delete(id);
        return ResponseEntity.noContent().build();
    }
}