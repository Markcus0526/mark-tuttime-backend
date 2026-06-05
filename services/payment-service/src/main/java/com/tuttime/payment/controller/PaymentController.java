package com.tuttime.payment.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.enums.PaymentStatus;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.payment.dto.PaymentDto;
import com.tuttime.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Controller", description = "YooKassa (YooMoney) Integration")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/checkout")
    @Operation(summary = "Create a YooKassa payment session")
    public ResponseEntity<ApiRes<String>> createSession(
            @Valid @RequestBody PaymentDto.PaymentRequest request) {

        validateLogin();

        String url = paymentService.initiatePaymentLifecycle(
                request.bookingId(),
                request.userId(),
                request.branchId(),
                request.amount()
        );
        return ResponseEntity.ok(ApiRes.success(url));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Handle YooKassa background notifications")
    public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
        paymentService.synchronizeTransactionState(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status/{bookingId}")
    @Operation(summary = "Fetch internal status for Mobile UI")
    public ResponseEntity<ApiRes<String>> getStatus(@PathVariable UUID bookingId) {

        validateLogin();

        String status = paymentService.fetchPaymentStatus(bookingId);
        return ResponseEntity.ok(ApiRes.success(status));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Execute refund from Admin Panel")
    public ResponseEntity<ApiRes<String>> triggerRefund(
            @PathVariable UUID paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String reason) {

        validateAdminOrOwner();

        boolean status = paymentService.processAdministrativeRefund(paymentId, amount, reason);
        return ResponseEntity.ok(ApiRes.success(status ? "SUCCESS" : "FAILED"));
    }

    @PostMapping("/test/confirm-booking/{bookingId}")
    @Operation(summary = "FOR TESTING: Manually confirms a YooKassa payment and triggers Kafka event")
    @Profile("!production")
    public ResponseEntity<ApiRes<String>> testConfirmPayment(@PathVariable UUID bookingId) {
        // In dev mode, we skip YooKassa and force the 'COMPLETED' state
        String mockYooId = "test_pay_" + UUID.randomUUID();

        // Controller calls the renamed service method
        paymentService.testManualPaymentSuccess(bookingId, mockYooId);

        return ResponseEntity.ok(ApiRes.success("Success: Kafka event sent for booking " + bookingId));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Admin: List payments for a branch (with filters)")
    public ResponseEntity<ApiRes<Page<PaymentDto.AdminPaymentResponse>>> listBranchPayments(
            @PathVariable UUID branchId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) OffsetDateTime fromDate,
            @RequestParam(required = false) OffsetDateTime toDate,
            @RequestParam(required = false) String query, // search by bookingId string
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        Pageable pageable = PageRequest.of(page, size,
                direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());
        Page<PaymentDto.AdminPaymentResponse> result = paymentService.listBranchPayments(
                branchId, status, fromDate, toDate, query, pageable, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(result));
    }

    @GetMapping("/admin/{paymentId}")
    @Operation(summary = "Admin: Full payment details (including refunds)")
    public ResponseEntity<ApiRes<PaymentDto.AdminPaymentDetailResponse>> getPaymentForAdmin(
            @PathVariable UUID paymentId) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        PaymentDto.AdminPaymentDetailResponse result = paymentService.getPaymentForAdmin(paymentId, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(result));
    }

    @GetMapping("/branch/{branchId}/summary")
    @Operation(summary = "Admin: Payment statistics (today, week, month, pending, refunded)")
    public ResponseEntity<ApiRes<PaymentDto.BranchPaymentSummary>> getBranchPaymentSummary(
            @PathVariable UUID branchId) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        PaymentDto.BranchPaymentSummary summary = paymentService.getBranchPaymentSummary(branchId, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(summary));
    }

    @GetMapping("/branch/{branchId}/payouts")
    @Operation(summary = "Admin: List completed payments ready for settlement")
    public ResponseEntity<ApiRes<Page<PaymentDto.PayoutResponse>>> listPayouts(
            @PathVariable UUID branchId,
            @RequestParam(required = false) OffsetDateTime fromDate,
            @RequestParam(required = false) OffsetDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        Page<PaymentDto.PayoutResponse> result = paymentService.listPayouts(branchId, fromDate, toDate, pageable, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(result));
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
