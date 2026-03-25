package com.tuttime.order.controller;

import com.tuttime.order.dto.OrderStatusUpdate;
import com.tuttime.order.dto.PreOrderRequest;
import com.tuttime.order.entity.Order;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@PostMapping("/preorder")
	public ResponseEntity<Order> createPreOrder(@RequestBody PreOrderRequest request) {
		return ResponseEntity.ok(orderService.createPreOrder(request));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<Order> updateStatus(@PathVariable Long id, @RequestBody OrderStatusUpdate update) {
		return ResponseEntity.ok(orderService.updateStatus(id, update));
	}

}
