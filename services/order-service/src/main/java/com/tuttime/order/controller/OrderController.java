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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
    public ResponseEntity<ApiRes<OrderDto.OrderDetailResponse>> getOrder(@PathVariable UUID orderId) {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        // The service should verify if the logged-in user owns this order
        OrderDto.OrderDetailResponse response = orderService.getOrderById(orderId, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(response));
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get order by booking ID", description = "Retrieves the active order associated with a specific table booking")
    public ResponseEntity<ApiRes<OrderDto.OrderDetailResponse>> getOrderByBookingId(
            @PathVariable UUID bookingId) {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        OrderDto.OrderDetailResponse response = orderService.getOrderByBookingId(bookingId, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(response));
    }

    @PutMapping("/{orderId}")
    @Operation(summary = "Modify order content", description = "Replaces items and recalculates totals. Allowed only while status is CREATED or PAID.")
    public ResponseEntity<ApiRes<OrderDto.OrderResponse>> updateOrderContent(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderDto.OrderUpdateRequest request) {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        OrderDto.OrderResponse response = orderService.updateOrderContent(orderId, request, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(response));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", description = "Soft-cancels the order. Only allowed before SENT_TO_RESTAURANT.")
    public ResponseEntity<ApiRes<OrderDto.OrderResponse>> cancelOrder(
            @PathVariable UUID orderId) {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        OrderDto.OrderResponse response = orderService.cancelOrder(orderId, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(response));
    }

    // --- STAFF & MANAGEMENT ENDPOINTS ---

    @PatchMapping("/status/bulk")
    @Operation(summary = "Update order status for multiple orders", description = "Transitions order lifecycle for a list of orders. Restricted to Staff/Admin.")
    public ResponseEntity<ApiRes<List<OrderDto.OrderResponse>>> updateStatusBulk(
            @RequestBody List<UUID> orderIds,
            @RequestParam OrderStatus status) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        List<OrderDto.OrderResponse> responses = orderService.updateStatus(orderIds, status, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(responses));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status", description = "Transitions order through lifecycle. Restricted to Staff/Admin.")
    public ResponseEntity<ApiRes<OrderDto.OrderResponse>> updateStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus status) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        List<OrderDto.OrderResponse> responses = orderService.updateStatus(List.of(orderId), status, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(responses.getFirst()));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Search orders", description = "Filter by branch, status, amount range, or guest name/phone.")
    public ResponseEntity<ApiRes<Page<OrderDto.OrderDetailResponse>>> listAndSearchOrders(
            @PathVariable UUID branchId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDateTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDateTime,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        Sort sortOrder = direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<OrderDto.OrderDetailResponse> orders = orderService.searchOrders(
                branchId, query, status, startDateTime, endDateTime, minAmount, maxAmount, pageable,
                currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(orders));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders of a specific user (owner/manager only)",
            description = "For branch staff: returns orders of a user within a branch. The branchId must be provided for staff; admins can see all.")
    public ResponseEntity<ApiRes<Page<OrderDto.OrderDetailResponse>>> getOrdersByUserId(
            @PathVariable UUID userId,
            @RequestParam UUID branchId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validateAdminOrOwnerOrManager(); // only staff can access this endpoint
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        Page<OrderDto.OrderDetailResponse> orders = orderService.getOrdersByUserId(
                userId, branchId, status, page, size, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(orders));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my orders", description = "Returns paginated list of orders for the currently authenticated user, including branch details.")
    public ResponseEntity<ApiRes<Page<OrderDto.OrderDetailResponse>>> getMyOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

        Page<OrderDto.OrderDetailResponse> orders = orderService.getMyOrders(currentUserId, status, page, size);
        return ResponseEntity.ok(ApiRes.success(orders));
    }

    // --- INTERNAL / POS SYNC ---

    @PostMapping("/{orderId}/pos-sync")
    public ResponseEntity<ApiRes<OrderDto.OrderResponse>> syncFromPos(
            @PathVariable UUID orderId,
            @RequestBody OrderDto.PosSyncRequest syncRequest,
            @RequestHeader("X-API-Key") String apiKey) {
        OrderDto.OrderResponse response = orderService.syncStatusFromPos(orderId, syncRequest, apiKey);
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