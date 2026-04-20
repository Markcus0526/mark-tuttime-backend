package com.tuttime.order.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Controller", description = "APIs for managing restaurant pre-orders and statuses")
public class OrderController {

	private final OrderService orderService;

	// --- USER ENDPOINTS ---

	@GetMapping("/{orderId}")
	@Operation(summary = "Get order details", description = "Retrieves full order details. Users can only see their own orders.")
	public ResponseEntity<ApiRes<OrderDto.OrderResponse>> getOrder(@PathVariable UUID orderId) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		// The service should verify if the logged-in user owns this order
		OrderDto.OrderResponse response = orderService.getOrderById(orderId, currentUserId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@GetMapping("/my-orders")
	@Operation(summary = "Get current user's orders", description = "Returns a paginated list of orders for the logged-in user")
	public ResponseEntity<ApiRes<Page<OrderDto.OrderResponse>>> getMyOrders(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		return ResponseEntity.ok(ApiRes.success(orderService.getOrdersByUserId(currentUserId, page, size)));
	}

	@GetMapping("/booking/{bookingId}")
	@Operation(summary = "Get order by booking ID", description = "Retrieves the active order associated with a specific table booking")
	public ResponseEntity<ApiRes<OrderDto.OrderResponse>> getOrderByBookingId(
			@PathVariable UUID bookingId) {

		validateLogin();

		OrderDto.OrderResponse response = orderService.getOrderByBookingId(bookingId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PutMapping("/{orderId}")
	@Operation(summary = "Modify order content", description = "Replaces items and recalculates totals. Allowed only while status is CREATED or PAID.")
	public ResponseEntity<ApiRes<OrderDto.OrderResponse>> updateOrderContent(
			@PathVariable UUID orderId,
			@Valid @RequestBody OrderDto.OrderUpdateRequest request) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		OrderDto.OrderResponse response = orderService.updateOrderContent(orderId, request, currentUserId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@PostMapping("/{orderId}/cancel")
	@Operation(summary = "Cancel an order", description = "Soft-cancels the order. Only allowed before SENT_TO_RESTAURANT.")
	public ResponseEntity<ApiRes<OrderDto.OrderResponse>> cancelOrder(
			@PathVariable UUID orderId) {

		validateLogin();
		UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

		OrderDto.OrderResponse response = orderService.cancelOrder(orderId, currentUserId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	// --- STAFF & MANAGEMENT ENDPOINTS ---

	@PatchMapping("/{orderId}/status")
	@Operation(summary = "Update order status", description = "Transitions order through lifecycle. Restricted to Staff/Admin.")
	public ResponseEntity<ApiRes<OrderDto.OrderResponse>> updateStatus(
			@PathVariable UUID orderId,
			@RequestParam OrderStatus status) {

		validateAdminOrOwnerOrManager();

		OrderDto.OrderResponse response = orderService.updateStatus(orderId, status);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@GetMapping("/search")
	@Operation(summary = "Search orders (Admin)", description = "Filter by branch, status, or date range.")
	public ResponseEntity<ApiRes<Page<OrderDto.OrderResponse>>> searchOrders(
			@RequestParam(required = false) UUID branchId,
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {

		validateAdminOrOwnerOrManager();

		return ResponseEntity.ok(ApiRes.success(orderService.searchOrders(branchId, status, page, size)));
	}

	// --- INTERNAL / POS SYNC ---

	@PostMapping("/{orderId}/pos-sync")
	@Operation(summary = "POS Kitchen Update", description = "Endpoint for POS Service to update status based on kitchen activity.")
	public ResponseEntity<ApiRes<OrderDto.OrderResponse>> syncFromPos(
			@PathVariable UUID orderId,
			@RequestBody OrderDto.PosSyncRequest syncRequest) {
		
		// In a real microservice, you'd validate an API Key or Service Token here
		OrderDto.OrderResponse response = orderService.syncStatusFromPos(orderId, syncRequest);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	/**
	 * Private helper to keep the controller DRY
	 */
	private void validateLogin() {
		if (!UserContext.isValidUUID()) {
			throw new UnauthorizedException("Unauthorized: You should login");
		}
	}

	private void validateAdmin() {
		if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}
}