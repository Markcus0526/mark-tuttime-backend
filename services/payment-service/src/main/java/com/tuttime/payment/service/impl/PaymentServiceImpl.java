package com.tuttime.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.PaymentStatus;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.event.PaymentEvent;
import com.tuttime.common.event.proto.GetUserDetailRequest;
import com.tuttime.common.event.proto.GetUserDetailResponse;
import com.tuttime.common.event.proto.GetUserDetailsRequest;
import com.tuttime.common.event.proto.GetUserDetailsResponse;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.payment.dto.PaymentDto;
import com.tuttime.payment.dto.PaymentMethodDto;
import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.grpc.client.RestaurantGrpcClient;
import com.tuttime.payment.grpc.client.UserGrpcClient;
import com.tuttime.payment.repository.PaymentAuditLogRepository;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.repository.RefundRepository;
import com.tuttime.payment.repository.projection.BranchPaymentSummaryProjection;
import com.tuttime.payment.service.PaymentMethodService;
import com.tuttime.payment.service.PaymentService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.pojo.Amount;
import ru.loolzaaa.youkassa.pojo.Confirmation;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;
import ru.loolzaaa.youkassa.processors.RefundProcessor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final PaymentAuditLogRepository paymentAuditLogRepository;
    private final PaymentMethodService paymentMethodService;
    private final RestaurantGrpcClient restaurantGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final EventProducer eventProducer;
    private final ApiClient apiKassaClient;
    private final ObjectMapper objectMapper;

    @Value("${yookassa.shop-id}")
    private String shopId;

    @Value("${yookassa.secret-key}")
    private String secretKey;

    @Override
    @Transactional
    public String initiatePaymentLifecycle(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount) {
        // 1. Check for existing PENDING payment
        var existing = paymentRepository.findByBookingId(bookingId);
        if (existing.isPresent() && PaymentStatus.PENDING == existing.get().getStatus()) {
            return existing.get().getConfirmationUrl();
        }

        // 2. SIMULATION CHECK: If no keys, return a mock URL immediately
        // In a real project, you might use a Spring Profile like @Profile("dev")
        if (apiKassaClient == null || "test_placeholder_key_12345".equals(secretKey)) {
            return createMockPayment(bookingId, userId, branchId, amount);
        }

        // Otherwise, proceed with real YooKassa call
        return createNewPayment(bookingId, userId, branchId, amount);
    }

    @Override
    @Transactional
    public void synchronizeTransactionState(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventId = root.get("id").asText();

            // 1. Idempotency Check (Production critical)
            if (paymentAuditLogRepository.existsByEventId(eventId)) return;

            JsonNode obj = root.get("object");
            String yooId = obj.get("id").asText();
            String yooStatus = obj.get("status").asText().toUpperCase();

            paymentRepository.findByYookassaPaymentId(yooId).ifPresent(payment -> {
                // 2. Update Status mapping
                if ("SUCCEEDED".equals(yooStatus)) {
                    payment.setStatus(PaymentStatus.COMPLETED);

                    // Check if YooKassa returned a saved payment method in this webhook
                    if (obj.has("payment_method")) {
                        JsonNode pm = obj.get("payment_method");
                        // Only save if it's a card and the 'saved' flag is true
                        if (pm.get("saved").asBoolean() && "bank_card".equals(pm.get("type").asText())) {

                            PaymentMethodDto.YooKassaPaymentMethod methodDto = mapToPaymentMethodDto(pm);
                            // Save to user_payment_methods table
                            paymentMethodService.savePaymentMethod(payment.getUserId(), methodDto);
                        }
                    }
                } else if ("CANCELED".equals(yooStatus)) {
                    payment.setStatus(PaymentStatus.FAILED);
                }

                // 3. Save DB first (Atomic requirement)
                paymentRepository.save(payment);

                PaymentAuditLog audit = new PaymentAuditLog();
                audit.setPaymentId(payment.getId());
                audit.setEventId(eventId);
                audit.setEventType(root.get("event").asText());
                audit.setEventData(rawPayload);
                paymentAuditLogRepository.save(audit);

                // 4. Trigger Event ONLY if status is COMPLETED
                dispatchPaymentCompletedEvent(payment, yooId);

                log.info("Payment {} synchronized to status {}", yooId, payment.getStatus());
            });
        } catch (Exception e) {
            log.error("Webhook synchronization failed", e);
        }
    }

    @Override
    @Transactional
    public boolean processAdministrativeRefund(UUID paymentId, BigDecimal amount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new InvalidRequestException("Payment not found"));

        RefundProcessor processor = new RefundProcessor(apiKassaClient);
        String idempotencyKey = UUID.randomUUID().toString();

        var yooRefund = processor.create(ru.loolzaaa.youkassa.model.Refund.builder()
                .paymentId(payment.getYookassaPaymentId())
                .amount(new Amount(amount.toString(), "RUB"))
                .build(), idempotencyKey);

        Refund refund = refundRepository.save(Refund.builder()
                .payment(payment)
                .yookassaRefundId(yooRefund.getId())
                .amount(amount)
                .status(yooRefund.getStatus().toUpperCase())
                .reason(reason)
                .build());

        dispatchRefundCompletedEvent(payment, reason);

        return refund.getStatus().equals("SUCCESS");
    }

    @Override
    public String fetchPaymentStatus(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .map(p -> p.getStatus().name())
                .orElse("NOT_FOUND");
    }

    @Override
    @Transactional
    public void testManualPaymentSuccess(UUID bookingId, String mockYooId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    log.info("[TEST] Creating mock payment record for booking: {}", bookingId);
                    Payment mock = Payment.builder()
                            .bookingId(bookingId)
                            .userId(UUID.randomUUID())
                            .branchId(UUID.randomUUID())
                            .amount(new BigDecimal("100.00"))
                            .currency("RUB")
                            .status(PaymentStatus.PENDING)
                            .build();
                    return paymentRepository.save(mock);
                });

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setYookassaPaymentId(mockYooId);
        payment.setPaymentMethodType("manual_test_card");
        paymentRepository.save(payment);

        dispatchPaymentCompletedEvent(payment, mockYooId);

        log.info("[TEST] Simulated success for booking {}", bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDto.AdminPaymentResponse> listBranchPayments(
            UUID branchId,
            PaymentStatus status,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            String query,
            Pageable pageable,
            UUID currentUserId,
            String userRoles) {

        // 1. Permission check
        restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

        // 2. Build dynamic Specification (as before)
        Specification<Payment> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("branchId"), branchId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            if (toDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            if (query != null && !query.isEmpty()) {
                try {
                    UUID bookingUuid = UUID.fromString(query);
                    predicates.add(cb.equal(root.get("bookingId"), bookingUuid));
                } catch (IllegalArgumentException ignored) {
                    // not a valid UUID – ignore (or add search by user email/phone later)
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 3. Fetch payments page
        Page<Payment> paymentPage = paymentRepository.findAll(spec, pageable);
        if (paymentPage.isEmpty()) {
            return Page.empty();
        }

        // 4. Extract unique userIds and batch fetch user details
        List<UUID> userIds = paymentPage.getContent().stream()
                .map(Payment::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, GetUserDetailResponse> userDetailsMap = getUsersBatch(userIds);

        // 5. Map to AdminPaymentResponse DTO
        return paymentPage.map(payment -> {
            GetUserDetailResponse user = userDetailsMap.get(payment.getUserId());
            String displayName = "Unknown";
            String userEmail = null;
            String userPhone = null;

            if (user != null) {
                userEmail = user.getEmail();
                userPhone = user.getPhoneNumber();
                // Priority: full_name → email → phone → "Unknown"
                if (user.getFullName() != null && !user.getFullName().isBlank()) {
                    displayName = user.getFullName();
                } else if (userEmail != null && !userEmail.isBlank()) {
                    displayName = userEmail;
                } else if (userPhone != null && !userPhone.isBlank()) {
                    displayName = userPhone;
                }
            }

            BigDecimal commission = payment.getAmount().multiply(BigDecimal.valueOf(0.01));
            BigDecimal net = payment.getAmount().subtract(commission);

            return new PaymentDto.AdminPaymentResponse(
                    payment.getId(),
                    payment.getBookingId(),
                    displayName,
                    userEmail,
                    userPhone,
                    payment.getAmount(),
                    payment.getCurrency(),
                    payment.getStatus(),
                    payment.getPaymentMethodType(),
                    payment.getCreatedAt(),
                    payment.getUpdatedAt(),
                    commission,
                    net
            );
        });
    }

    // -----------------------------------------------------------------
    // 2. Get full payment details for admin (with refunds)
    // -----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public PaymentDto.AdminPaymentDetailResponse getPaymentForAdmin(UUID paymentId, UUID currentUserId, String userRoles) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new InvalidRequestException("Payment not found"));
        restaurantGrpcClient.validateBranchPermission(payment.getBranchId(), currentUserId, userRoles);

        List<Refund> refunds = refundRepository.findByPaymentId(paymentId);
        BigDecimal totalRefunded = refunds.stream()
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal commission = payment.getAmount().multiply(BigDecimal.valueOf(0.01));
        BigDecimal net = payment.getAmount().subtract(commission);

        // Fetch user details via gRPC
        var user = userGrpcClient.getUserDetail(GetUserDetailRequest.newBuilder()
                .setUserId(payment.getUserId().toString())
                .build());

        List<PaymentDto.RefundDto> refundDtos = refunds.stream()
                .map(r -> new PaymentDto.RefundDto(r.getId(), r.getAmount(), r.getStatus(), r.getReason(), r.getCreatedAt()))
                .collect(Collectors.toList());

        return new PaymentDto.AdminPaymentDetailResponse(
                payment.getId(),
                payment.getBookingId(),
                payment.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getYookassaPaymentId(),
                payment.getPaymentMethodType(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                refundDtos,
                totalRefunded,
                commission,
                net
        );
    }

    // -----------------------------------------------------------------
    // 3. Branch payment summary (dashboard stats)
    // -----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public PaymentDto.BranchPaymentSummary getBranchPaymentSummary(UUID branchId, UUID currentUserId, String userRoles) {
        restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

        BranchPaymentSummaryProjection proj = paymentRepository.getBranchPaymentSummary(branchId);
        return new PaymentDto.BranchPaymentSummary(
                proj.getTodayTotal(),
                proj.getTodayCount(),
                proj.getWeekTotal(),
                proj.getWeekCount(),
                proj.getMonthTotal(),
                proj.getMonthCount(),
                proj.getPendingTotal(),
                proj.getPendingCount(),
                proj.getRefundedTotal(),
                proj.getRefundedCount()
        );
    }

    // -----------------------------------------------------------------
    // 4. List payouts (completed payments)
    // -----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDto.PayoutResponse> listPayouts(UUID branchId, OffsetDateTime fromDate, OffsetDateTime toDate,
                                                       Pageable pageable, UUID currentUserId, String userRoles) {
        restaurantGrpcClient.validateBranchPermission(branchId, currentUserId, userRoles);

        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("branchId"), branchId));
            predicates.add(cb.equal(root.get("status"), PaymentStatus.COMPLETED));
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("updatedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("updatedAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Payment> paymentPage = paymentRepository.findAll(spec, pageable);

        return paymentPage.map(payment -> {
            BigDecimal commission = payment.getAmount().multiply(BigDecimal.valueOf(0.01));
            BigDecimal net = payment.getAmount().subtract(commission);
            return new PaymentDto.PayoutResponse(
                    payment.getId(),
                    payment.getUpdatedAt(),
                    payment.getAmount(),
                    commission,
                    net,
                    payment.getYookassaPaymentId()
            );
        });
    }

    // -----------------------------------------------------------------
    // Helper to fetch user names via gRPC (batch)
    // -----------------------------------------------------------------
    public Map<UUID, GetUserDetailResponse> getUsersBatch(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        // Build request with all userIds as strings
        GetUserDetailsRequest request = GetUserDetailsRequest.newBuilder()
                .addAllUserIds(userIds.stream().map(UUID::toString).collect(Collectors.toList()))
                .build();
        // Call batch RPC (note: method is still called getUserDetails)
        GetUserDetailsResponse response = userGrpcClient.getUserDetails(request);
        // Map to UUID -> UserDetailResponse
        return response.getProfilesList().stream()
                .collect(Collectors.toMap(
                        profile -> UUID.fromString(profile.getUserId()),
                        profile -> profile
                ));
    }

    private String createNewPayment(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount) {
        PaymentProcessor processor = new PaymentProcessor(apiKassaClient);
        UUID idempotencyKey = UUID.randomUUID();

        var yooRequest = ru.loolzaaa.youkassa.model.Payment.builder()
                .amount(new Amount(amount.toString(), "RUB"))
                .confirmation(Confirmation.builder()
                        .type("redirect")
                        .returnUrl("tuttime://checkout/complete/" + bookingId)
                        .build())
                .capture(true)
                .description("Booking " + bookingId)
                .build();

        var yooResponse = processor.create(yooRequest, idempotencyKey.toString());

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .userId(userId)
                .branchId(branchId)
                .amount(amount)
                .currency("RUB")
                .yookassaPaymentId(yooResponse.getId())
                .idempotencyKey(idempotencyKey)
                .confirmationUrl(yooResponse.getConfirmation().getConfirmationUrl())
                .status(PaymentStatus.PENDING)
                .build();

        return paymentRepository.save(payment).getConfirmationUrl();
    }

    private String createMockPayment(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount) {
        String mockYooId = "mock_pay_" + UUID.randomUUID();
        String mockUrl = "https://example.com" + mockYooId;

        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .userId(userId)
                .branchId(branchId)
                .amount(amount)
                .currency("RUB")
                .yookassaPaymentId(mockYooId)
                .idempotencyKey(UUID.randomUUID())
                .confirmationUrl(mockUrl)
                .status(PaymentStatus.PENDING)
                .build();

        return paymentRepository.save(payment).getConfirmationUrl();
    }

    /**
     * Shared helper to ensure Kafka events are consistent between Prod and Test
     */
    private void dispatchPaymentCompletedEvent(Payment payment, String externalId) {
        PaymentEvent.PaymentCompletedEvent event = PaymentEvent.PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .branchId(payment.getBranchId())
                .externalPaymentId(externalId)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .commissionRate(BigDecimal.valueOf(0.01))
                .status(payment.getStatus())
                .build();
        eventProducer.sendPaymentCompletedEvent(event);
    }

    private void dispatchRefundCompletedEvent(Payment payment, String reason) {
        PaymentEvent.PaymentRefundedEvent event = PaymentEvent.PaymentRefundedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .branchId(payment.getBranchId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .reason(reason)
                .build();
        eventProducer.sendPaymentRefundEvent(event);
    }

    private PaymentMethodDto.YooKassaPaymentMethod mapToPaymentMethodDto(JsonNode pm) {
        JsonNode card = pm.get("card");
        return PaymentMethodDto.YooKassaPaymentMethod.builder()
                .id(pm.get("id").asText())
                .saved(pm.get("saved").asBoolean())
                .card(new PaymentMethodDto.CardDetails(
                        card.get("first6").asText(),
                        card.get("last4").asText(),
                        card.get("expiry_month").asText(),
                        card.get("expiry_year").asText(),
                        card.get("card_type").asText(),
                        card.has("issuer_name") ? card.get("issuer_name").asText() : "Unknown"
                ))
                .build();
    }
}
