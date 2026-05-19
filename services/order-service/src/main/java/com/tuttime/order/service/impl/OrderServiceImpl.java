package com.tuttime.order.service.impl;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.event.OrderEvent;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.order.dto.OrderDto;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import com.tuttime.order.grpc.client.RestaurantGrpcClient;
import com.tuttime.order.grpc.client.UserGrpcClient;
import com.tuttime.order.mapper.OrderMapper;
import com.tuttime.order.repository.OrderRepository;
import com.tuttime.order.service.OrderDiscountCalculator;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tuttime.common.util.UuidUtil.NIL_UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final EventProducer eventProducer;
    private final UserGrpcClient userGrpcClient;
    private final RestaurantGrpcClient restaurantGrpcClient;
    private final MenuGrpcClient menuGrpcClient;
    private final OrderDiscountCalculator discountCalculator;

    @Value("${pos.api.key:}")
    private String posApiKey;

    // ---------- Read Operations ----------
    @Override
    @Transactional(readOnly = true)
    public OrderDto.OrderDetailResponse getOrderById(UUID orderId, UUID currentUserId, String userRoles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidRequestException("Order not found"));
        if (!order.getUserId().equals(currentUserId)) {
            restaurantGrpcClient.validateBranchPermission(order.getBranchId(), currentUserId, userRoles);
        }
        return getEnrichedOrders(List.of(order)).getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto.OrderDetailResponse> getOrdersByUserId(UUID userId, UUID branchId, OrderStatus status,
                                                                int page, int size, UUID currentUserId, String userRoles) {
        restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByUserIdAndBranchIdAndStatusAndDeletedAtIsNull(userId, branchId, status, pageRequest);
        } else {
            orderPage = orderRepository.findByUserIdAndBranchIdAndDeletedAtIsNull(userId, branchId, pageRequest);
        }
        List<OrderDto.OrderDetailResponse> enrichedList = getEnrichedOrders(orderPage.getContent());
        return new PageImpl<>(enrichedList, pageRequest, orderPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto.OrderDetailResponse> getMyOrders(UUID userId, OrderStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage;
        if (status != null) {
            orderPage = orderRepository.findByUserIdAndStatusAndDeletedAtIsNull(userId, status, pageRequest);
        } else {
            orderPage = orderRepository.findByUserIdAndDeletedAtIsNull(userId, pageRequest);
        }
        List<OrderDto.OrderDetailResponse> enriched = getEnrichedOrders(orderPage.getContent());
        return new PageImpl<>(enriched, pageRequest, orderPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto.OrderDetailResponse getOrderByBookingId(UUID bookingId, UUID currentUserId, String userRoles) {
        Order order = orderRepository.findByBookingIdWithItems(bookingId)
                .orElseThrow(() -> new InvalidRequestException("No order found for booking: " + bookingId));
        if (!order.getUserId().equals(currentUserId)) {
            restaurantGrpcClient.validateBranchPermission(order.getBranchId(), currentUserId, userRoles);
        }
        return getEnrichedOrders(List.of(order)).getFirst();
    }

    // ---------- Write Operations ----------
    @Override
    @Transactional
    public OrderDto.OrderResponse updateOrderContent(UUID orderId, OrderDto.OrderUpdateRequest request,
                                                     UUID currentUserId, String userRoles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidRequestException("Order not found"));
        if (!order.getUserId().equals(currentUserId)) {
            restaurantGrpcClient.validateBranchPermission(order.getBranchId(), currentUserId, userRoles);
        }
        if (order.getStatus() == OrderStatus.SENT_TO_RESTAURANT
                || order.getStatus().ordinal() > OrderStatus.SENT_TO_RESTAURANT.ordinal()) {
            throw new InvalidRequestException("Cannot modify order after it has been accepted by the restaurant");
        }

        order.getItems().clear();
        for (var itemReq : request.items()) {
            List<UUID> modIds = (itemReq.modifiers() != null)
                    ? itemReq.modifiers().stream().map(m -> m.modifierId()).toList()
                    : Collections.emptyList();
            BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(itemReq.menuItemId(), modIds);
            OrderItem newItem = OrderItem.builder()
                    .menuItemId(itemReq.menuItemId())
                    .quantity(itemReq.quantity())
                    .price(verifiedPrice)
                    .order(order)
                    .build();
            if (itemReq.modifiers() != null) {
                itemReq.modifiers().forEach(mod -> {
                    newItem.getModifiers().add(OrderItemModifier.builder()
                            .modifierId(mod.modifierId())
                            .priceAdjustment(mod.priceAdjustment())
                            .orderItem(newItem)
                            .build());
                });
            }
            order.addItem(newItem);
        }
        BigDecimal finalTotal = discountCalculator.calculateFinalTotal(order.getBranchId(), order.getItems());
        order.setTotalAmount(finalTotal);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public List<OrderDto.OrderResponse> updateStatus(List<UUID> orderIds, OrderStatus newStatus, UUID currentUserId, String userRoles) {
        List<OrderDto.OrderResponse> responses = new ArrayList<>();
        for (UUID orderId : orderIds) {
            Order order = orderRepository.findByIdWithLock(orderId)
                    .orElseThrow(() -> new InvalidRequestException("Order not found: " + orderId));
            restaurantGrpcClient.validateBranchPermission(order.getBranchId(), currentUserId, userRoles);
            OrderStatus oldStatus = order.getStatus();
            order.setStatus(newStatus);
            // No explicit save – dirty checking will update
            OrderEvent.OrderStatusEvent event = OrderEvent.OrderStatusEvent.builder()
                    .orderId(order.getId())
                    .bookingId(order.getBookingId())
                    .branchId(order.getBranchId())
                    .oldStatus(oldStatus)
                    .newStatus(newStatus)
                    .build();
            eventProducer.sendOrderStatusUpdatedEvent(event);
            responses.add(orderMapper.toResponse(order));
        }
        return responses;
    }

    @Override
    @Transactional
    public OrderDto.OrderResponse cancelOrder(UUID orderId, UUID currentUserId, String userRoles) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidRequestException("Order not found"));
        if (order.getStatus().ordinal() >= OrderStatus.ACCEPTED.ordinal()) {
            throw new InvalidRequestException("Order is already with the kitchen and cannot be canceled");
        }
        restaurantGrpcClient.validateBranchPermission(order.getBranchId(), currentUserId, userRoles);
        order.setStatus(OrderStatus.CANCELED);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto.OrderResponse syncStatusFromPos(UUID orderId, OrderDto.PosSyncRequest request, String apiKey) {
        if (!posApiKey.equals(apiKey)) {
            throw new InvalidRequestException("Invalid API key");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new InvalidRequestException("Order not found"));
        order.setStatus(request.posStatus());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDto.OrderDetailResponse> searchOrders(UUID branchId, String query, OrderStatus status,
                                                           OffsetDateTime startDateTime, OffsetDateTime endDateTime,
                                                           BigDecimal minAmount, BigDecimal maxAmount,
                                                           Pageable pageable, UUID currentUserId, String userRoles) {
        restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

        Page<Order> orders = orderRepository.searchOrders(branchId, query, status,
                startDateTime, endDateTime,
                minAmount, maxAmount, pageable);
        List<OrderDto.OrderDetailResponse> enriched = getEnrichedOrders(orders.getContent());
        return new PageImpl<>(enriched, pageable, orders.getTotalElements());
    }

    @Override
    @Transactional
    public void deleteOrder(UUID id) {
        orderRepository.deleteById(id); // soft delete via @SQLDelete
    }

    // ---------- Saga / Internal ----------
    @Override
    @Transactional
    public Order createOrUpdateOrder(UUID bookingId, UUID userId, UUID branchId) {
        try {
            return orderRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
                    .map(order -> {
                        if (order.getStatus().ordinal() >= OrderStatus.CREATED.ordinal()) {
                            log.warn("Order for booking {} is already in status: {}", bookingId, order.getStatus());
                            return order;
                        }
                        order.setStatus(OrderStatus.CREATED);
                        return orderRepository.save(order);
                    })
                    .orElseGet(() -> {
                        Order newOrder = Order.builder()
                                .bookingId(bookingId)
                                .userId(userId)
                                .branchId(branchId)
                                .status(OrderStatus.CREATED)
                                .totalAmount(BigDecimal.ZERO)
                                .build();
                        return orderRepository.save(newOrder);
                    });
        } catch (DataIntegrityViolationException e) {
            // Duplicate bookingId – retry fetch
            return orderRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
                    .orElseThrow(() -> new InvalidRequestException("Failed to create or retrieve order for booking"));
        }
    }

    @Override
    @Transactional
    public void cancelOrderByBookingId(UUID bookingId) {
        log.info("Attempting to cancel order for booking: {}", bookingId);
        Order order = orderRepository.findByBookingIdAndDeletedAtIsNull(bookingId)
                .orElseThrow(() -> new InvalidRequestException("No active order found for booking: " + bookingId));
        if (order.getStatus().ordinal() >= OrderStatus.PREPARING.ordinal()) {
            log.warn("Cannot auto-cancel order {}. It is already in status: {}", order.getId(), order.getStatus());
            return;
        }
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        log.info("Order {} successfully canceled due to booking cancellation event.", order.getId());
    }

    @Override
    @Transactional
    public void updateStatusFromPayment(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new InvalidRequestException("Order not found"));
        // Optionally add branch permission check if this method is exposed via API
        // restaurantGrpcClient.validateBranchPermission(order.getBranchId(), ...);
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        OrderEvent.OrderStatusEvent event = OrderEvent.OrderStatusEvent.builder()
                .orderId(order.getId())
                .bookingId(order.getBookingId())
                .branchId(order.getBranchId())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .build();
        eventProducer.sendOrderStatusUpdatedEvent(event);
    }

    private List<OrderDto.OrderDetailResponse> getEnrichedOrders(List<Order> orders) {
        Set<String> userIds = orders.stream().map(o -> o.getUserId().toString()).collect(Collectors.toSet());
        // ✅ Collect all table IDs from all orders (flatten the list)
        Set<String> tableIds = orders.stream()
                .flatMap(o -> o.getTableIds().stream())
                .map(UUID::toString)
                .collect(Collectors.toSet());
        List<String> menuItemIds = orders.stream()
                .flatMap(o -> o.getItems().stream().map(i -> i.getMenuItemId().toString()))
                .distinct().toList();

        var usersFuture = CompletableFuture.supplyAsync(() -> fetchUsersMap(userIds));
        var tablesFuture = CompletableFuture.supplyAsync(() -> fetchTablesMap(tableIds));
        var menusFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return menuGrpcClient.getMenuDetails(menuItemIds).stream()
                        .collect(Collectors.toMap(MenuItemDetailPto::getMenuItemId, m -> m, (a, b) -> a));
            } catch (Exception e) {
                log.warn("Menu service failed, proceeding with empty menu data");
                return Collections.<String, MenuItemDetailPto>emptyMap();
            }
        });

        try {
            CompletableFuture.allOf(usersFuture, tablesFuture, menusFuture)
                    .orTimeout(5, TimeUnit.SECONDS)
                    .join();
        } catch (Exception e) {
            log.error("Timeout or error while enriching orders", e);
        }

        var userMap = usersFuture.join();
        var tableMap = tablesFuture.join();
        var menuMap = menusFuture.join();

        return orders.stream().map(order -> {
            var user = userMap.getOrDefault(order.getUserId().toString(),
                    GetUserDetailResponse.newBuilder().setUserId(NIL_UUID).setFullName("Deleted User").build());
            // ✅ Build list of TableHierarchyResponse for this order's table IDs
            List<GetTableHierarchyDetailResponse> tables = order.getTableIds().stream()
                    .map(tid -> tableMap.getOrDefault(tid.toString(),
                            GetTableHierarchyDetailResponse.newBuilder().setTableId(tid.toString()).setTableLabel("N/A").build()))
                    .toList();
            var orderMenus = order.getItems().stream()
                    .map(i -> menuMap.get(i.getMenuItemId().toString()))
                    .filter(Objects::nonNull)
                    .toList();
            // ✅ Pass the list of tables to the mapper (signature changed)
            return orderMapper.toDetailResponse(order, user, tables, orderMenus);
        }).toList();
    }

    private Map<String, GetUserDetailResponse> fetchUsersMap(Set<String> ids) {
        return ids.parallelStream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    try {
                        return userGrpcClient.getUserDetail(GetUserDetailRequest.newBuilder().setUserId(id).build());
                    } catch (Exception e) {
                        return GetUserDetailResponse.newBuilder().setUserId(NIL_UUID).build();
                    }
                }
        ));
    }

    private Map<String, GetTableHierarchyDetailResponse> fetchTablesMap(Set<String> ids) {
        return ids.parallelStream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    try {
                        return restaurantGrpcClient.getTableHierarchyDetail(GetTableHierarchyDetailRequest.newBuilder().setTableId(id).build());
                    } catch (Exception e) {
                        return GetTableHierarchyDetailResponse.newBuilder()
                                .setTableId(NIL_UUID)
                                .setZoneId(NIL_UUID)
                                .setBranchId(NIL_UUID)
                                .setRestaurantId(NIL_UUID)
                                .setTableLabel("N/A")
                                .setZoneName("Unknown")
                                .setRestaurantName("Unknown")
                                .setBranchName("Unknown")
                                .setBranchAddress("N/A")
                                .setBranchPhone("N/A")
                                .build();
                    }
                }
        ));
    }
}