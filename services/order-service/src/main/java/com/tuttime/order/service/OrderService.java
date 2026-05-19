package com.tuttime.order.service;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    // User Operations
    OrderDto.OrderDetailResponse getOrderById(UUID orderId, UUID currentUserId, String userRoles);

    Page<OrderDto.OrderDetailResponse> getOrdersByUserId(UUID userId, UUID branchId, OrderStatus status,
                                                         int page, int size, UUID currentUserId, String userRoles);

    OrderDto.OrderDetailResponse getOrderByBookingId(UUID bookingId, UUID currentUserId, String userRoles);

    OrderDto.OrderResponse updateOrderContent(UUID orderId, OrderDto.OrderUpdateRequest request,
                                              UUID currentUserId, String userRoles);

    OrderDto.OrderResponse cancelOrder(UUID orderId, UUID currentUserId, String userRoles);

    List<OrderDto.OrderResponse> updateStatus(List<UUID> orderIds, OrderStatus newStatus, UUID currentUserId, String userRoles);

    OrderDto.OrderResponse syncStatusFromPos(UUID orderId, OrderDto.PosSyncRequest request, String apiKey);

    Page<OrderDto.OrderDetailResponse> searchOrders(UUID branchId, String query, OrderStatus status,
                                                    OffsetDateTime startDateTime, OffsetDateTime endDateTime,
                                                    BigDecimal minAmount, BigDecimal maxAmount,
                                                    Pageable pageable, UUID currentUserId, String userRoles);

    void deleteOrder(UUID orderId); // Soft delete

    Order createOrUpdateOrder(UUID bookingId, UUID userId, UUID branchId);

    void cancelOrderByBookingId(UUID bookingId);

    Page<OrderDto.OrderDetailResponse> getMyOrders(UUID userId, OrderStatus status, int page, int size);

    void updateStatusFromPayment(UUID orderId, OrderStatus newStatus);
}
