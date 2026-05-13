package com.tuttime.order.service;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.common.event.proto.*;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderItem;
import com.tuttime.order.entity.OrderItemModifier;
import com.tuttime.order.grpc.client.MenuGrpcClient;
import com.tuttime.order.grpc.client.PaymentGrpcClient;
import com.tuttime.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderGrpcDelegate {

	private final OrderRepository orderRepository;
	private final MenuGrpcClient menuGrpcClient;
	private final PaymentGrpcClient paymentGrpcClient;
	private final OrderDiscountCalculator discountCalculator;

	@Transactional
	public CreateOrderResponse createOrder(CreateOrderRequest request) {
		Order order = Order.builder()
				.bookingId(UuidUtil.toUuid(request.getBookingId()))
				.userId(UuidUtil.toUuid(request.getUserId()))
				.branchId(UuidUtil.toUuid(request.getBranchId()))
				.tableId(UuidUtil.toUuid(request.getTableId()))
				.guestName(request.getGuestName())
				.guestPhone(request.getGuestPhone())
				.specialRequest(request.getSpecialRequest())
				.status(OrderStatus.CREATED)
				.items(new ArrayList<>())
				.build();

		for (OrderItemRequest itemReq : request.getItemsList()) {
			List<UUID> modifierIds = itemReq.getModifiersList().stream()
					.map(m -> UuidUtil.toUuid(m.getModifierId()))
					.toList();

			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(
					UuidUtil.toUuid(itemReq.getMenuItemId()),
					modifierIds
			);

			OrderItem item = OrderItem.builder()
					.menuItemId(UuidUtil.toUuid(itemReq.getMenuItemId()))
					.quantity(itemReq.getQuantity())
					.price(verifiedPrice)
					.modifiers(new ArrayList<>())
					.order(order)
					.build();

			itemReq.getModifiersList().forEach(modReq -> {
				item.getModifiers().add(OrderItemModifier.builder()
						.modifierId(UuidUtil.toUuid(modReq.getModifierId()))
						.priceAdjustment(new BigDecimal(modReq.getPriceAdjustment()))
						.orderItem(item)
						.build());
			});

			order.addItem(item);
		}

		BigDecimal finalTotal = discountCalculator.calculateFinalTotal(order.getBranchId(), order.getItems());
		order.setTotalAmount(finalTotal);
		Order savedOrder = orderRepository.save(order);

		return CreateOrderResponse.newBuilder()
				.setOrderId(savedOrder.getId().toString())
				.setStatus(savedOrder.getStatus().name())
				.build();
	}

	@Transactional
	public UpdateOrderResponse updateOrder(UpdateOrderRequest request) {
		Order order = orderRepository.findById(UuidUtil.toUuid(request.getOrderId()))
				.orElseThrow(() -> new InvalidRequestException("Order not found: " + request.getOrderId()));

		if (order.getStatus() == OrderStatus.SERVED || order.getStatus() == OrderStatus.COMPLETED) {
			throw new InvalidRequestException("Cannot edit an order that is already served or completed.");
		}

		order.setGuestName(request.getGuestName());
		order.setGuestPhone(request.getGuestPhone());
		order.setSpecialRequest(request.getSpecialRequest());

		// Clear existing items (orphan removal should cascade)
		order.getItems().clear();

		for (OrderItemRequest itemReq : request.getItemsList()) {
			List<UUID> modifierIds = itemReq.getModifiersList().stream()
					.map(m -> UuidUtil.toUuid(m.getModifierId()))
					.toList();

			BigDecimal verifiedPrice = menuGrpcClient.getPriceForOrder(
					UuidUtil.toUuid(itemReq.getMenuItemId()),
					modifierIds
			);

			OrderItem item = OrderItem.builder()
					.menuItemId(UuidUtil.toUuid(itemReq.getMenuItemId()))
					.quantity(itemReq.getQuantity())
					.price(verifiedPrice)
					.modifiers(new ArrayList<>())
					.order(order)
					.build();

			itemReq.getModifiersList().forEach(modReq -> {
				item.getModifiers().add(OrderItemModifier.builder()
						.modifierId(UuidUtil.toUuid(modReq.getModifierId()))
						.priceAdjustment(new BigDecimal(modReq.getPriceAdjustment()))
						.orderItem(item)
						.build());
			});

			order.addItem(item);
		}

		BigDecimal finalTotal = discountCalculator.calculateFinalTotal(order.getBranchId(), order.getItems());
		order.setTotalAmount(finalTotal);
		orderRepository.save(order);

		return UpdateOrderResponse.newBuilder()
				.setSuccess(true)
				.setMessage("Order updated successfully")
				.build();
	}

	@Transactional(readOnly = true)
	public OrderDetailResponse getOrderDetail(OrderDetailRequest request) {
		Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		OrderDetailResponse.Builder responseBuilder = OrderDetailResponse.newBuilder()
				.setOrderId(order.getId().toString())
				.setBookingId(order.getBookingId().toString())
				.setTotalAmount(order.getTotalAmount().doubleValue());

		// In real code, fetch menu item names via gRPC to MenuService
		order.getItems().forEach(item -> {
			responseBuilder.addItems(OrderItemDetailResponse.newBuilder()
					.setMenuItemName("Burger") // placeholder
					.setQuantity(item.getQuantity())
					.setPrice(item.getPrice().doubleValue())
					.build());
		});

		return responseBuilder.build();
	}

	@Transactional
	public CancelOrderResponse cancelAndRefund(CancelOrderRequest request) {
		Order order = orderRepository.findById(UUID.fromString(request.getOrderId()))
				.orElseThrow(() -> new InvalidRequestException("Order not found"));

		CancelOrderResponse.Builder responseBuilder = CancelOrderResponse.newBuilder();

		if (order.getStatus() == OrderStatus.CANCELED) {
			return responseBuilder.setSuccess(false).setReason("Order is canceled.").build();
		}

		if (order.getStatus() == OrderStatus.SERVED || order.getStatus() == OrderStatus.COMPLETED) {
			return responseBuilder.setSuccess(false).setReason("Order is served or completed now.").build();
		}

		ProcessRefundRequest refundRequest = ProcessRefundRequest.newBuilder()
				.setBookingId(request.getBookingId())
				.setPaymentId(request.getPaymentId())
				.setAmount(order.getTotalAmount().toString())
				.setReason(StringUtils.hasText(request.getReason()) ? request.getReason() : "Canceled by user")
				.build();
		ProcessRefundResponse refundResponse = paymentGrpcClient.processRefund(refundRequest);

		if (refundResponse.getStatus()) {
			order.setStatus(OrderStatus.CANCELED);
			orderRepository.save(order);
		}

		responseBuilder.setSuccess(refundResponse.getStatus());
		if (!refundResponse.getStatus()) {
			responseBuilder.setReason("Refund is failed by Yoomoney.");
		}
		return responseBuilder.build();
	}

	@Transactional(readOnly = true)
	public GetTopItemsResponse getTopItems(GetTopItemsRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		LocalDate from = LocalDate.parse(request.getFromDate());
		LocalDate to = LocalDate.parse(request.getToDate());
		int limit = request.getLimit();

		OffsetDateTime fromDate = from.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
		OffsetDateTime toDate = to.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

		Pageable pageable = PageRequest.of(0, limit);
		Page<Object[]> results = orderRepository.findTopItemsByBranchAndDateRange(branchId, fromDate, toDate, pageable);

		GetTopItemsResponse.Builder responseBuilder = GetTopItemsResponse.newBuilder();
		for (Object[] row : results.getContent()) {
			TopItem item = TopItem.newBuilder()
					.setItemId(row[0].toString())
					.setQuantity(((Number) row[1]).intValue())
					.setRevenue(row[2].toString())
					.build();
			responseBuilder.addItems(item);
		}
		return responseBuilder.build();
	}

	@Transactional(readOnly = true)
	public GetRevenueBreakdownResponse getRevenueBreakdown(GetRevenueBreakdownRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		String granularity = request.getGranularity();
		LocalDate target = LocalDate.parse(request.getDate());
		OffsetDateTime targetDate = target.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

		List<RevenuePoint> points;
		BigDecimal totalGmv = BigDecimal.ZERO;
		int totalOrders = 0;

		if ("HOUR".equalsIgnoreCase(granularity)) {
			List<Object[]> rows = orderRepository.getHourlyRevenue(branchId, targetDate);
			points = rows.stream()
					.map(row -> RevenuePoint.newBuilder()
							.setPeriod(String.format("%02d:00", ((Number) row[0]).intValue()))
							.setGmv(row[1].toString())
							.setOrders(((Number) row[2]).intValue())
							.build())
					.collect(Collectors.toList());
			// Optionally calculate total for the day (sum of hours)
			totalGmv = points.stream().map(p -> new BigDecimal(p.getGmv())).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalOrders = points.stream().mapToInt(RevenuePoint::getOrders).sum();
		} else { // DAY
			List<Object[]> rows = orderRepository.getDailyRevenue(branchId, targetDate.minusDays(30), targetDate);
			points = rows.stream()
					.map(row -> RevenuePoint.newBuilder()
							.setPeriod(row[0].toString())
							.setGmv(row[1].toString())
							.setOrders(((Number) row[2]).intValue())
							.build())
					.collect(Collectors.toList());
			totalGmv = rows.stream().map(row -> (BigDecimal) row[1]).reduce(BigDecimal.ZERO, BigDecimal::add);
			totalOrders = rows.stream().mapToInt(row -> ((Number) row[2]).intValue()).sum();
		}

		return GetRevenueBreakdownResponse.newBuilder()
				.setGranularity(granularity)
				.addAllPoints(points)
				.setTotalGmv(totalGmv.toString())
				.setTotalOrders(totalOrders)
				.build();
	}

	@Transactional(readOnly = true)
	public GetMenuPerformanceResponse getMenuPerformance(GetMenuPerformanceRequest request) {
		UUID branchId = UUID.fromString(request.getBranchId());
		LocalDate start = LocalDate.parse(request.getStartDate());
		LocalDate end = LocalDate.parse(request.getEndDate());
		String groupBy = request.getGroupBy();

		OffsetDateTime startDate = start.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
		OffsetDateTime endDate = end.atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();

		GetMenuPerformanceResponse.Builder responseBuilder = GetMenuPerformanceResponse.newBuilder()
				.setGroupBy(groupBy);

		// Only ITEM grouping is directly supported (returns IDs)
		if ("ITEM".equalsIgnoreCase(groupBy)) {
			List<Object[]> results = orderRepository.getItemPerformance(branchId, startDate, endDate);
			for (Object[] row : results) {
				MenuPerformancePoint point = MenuPerformancePoint.newBuilder()
						.setId(row[0].toString())
						.setRevenue(row[1].toString())
						.setQuantity(((Number) row[2]).intValue())
						.build();
				responseBuilder.addPoints(point);
			}
		} else {
			// For CATEGORY, the order service cannot aggregate – return empty; analytics service will handle.
			// Or you could return the same ITEM results and let analytics service group.
		}
		return responseBuilder.build();
	}
}