package com.tuttime.payment.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import com.tuttime.payment.dto.PaymentMethodDto;
import com.tuttime.payment.service.PaymentMethodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Method Controller", description = "Add, Remove Payment Integration")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    // List Saved Cards (Method 1 & 2 Results)
    @GetMapping("/methods")
    @Operation(summary = "Get user saved cards", description = "Returns last4 and card type for the Profile and Select Payment pages.")
    public ResponseEntity<ApiRes<List<PaymentMethodDto.Response>>> getSavedCards() {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

        List<PaymentMethodDto.Response> cards = paymentMethodService.getUserPaymentMethods(currentUserId);
        return ResponseEntity.ok(ApiRes.success(cards));
    }

    // Explicitly Add Card via Profile (Method 2)
    @PostMapping("/methods/bind")
    @Operation(summary = "Add new card from Profile", description = "Triggers a 1 RUB verification to save a new card token.")
    public ResponseEntity<ApiRes<String>> bindNewCard() {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

        // Returns a confirmation_url for the user to verify their card
        String confirmationUrl = paymentMethodService.initializeCardBinding(currentUserId);
        return ResponseEntity.ok(ApiRes.success(confirmationUrl));
    }

    // Delete Payment Info
    @DeleteMapping("/methods/{methodId}")
    @Operation(summary = "Remove saved payment info")
    public ResponseEntity<ApiRes<Void>> deleteCard(@PathVariable UUID methodId) {

        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

        paymentMethodService.removePaymentMethod(currentUserId, methodId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
    }

    @PatchMapping("/methods/{methodId}/default")
    @Operation(summary = "Set primary card", description = "Used by the toggle on the Profile/Settings page.")
    public ResponseEntity<ApiRes<Void>> setDefaultCard(@PathVariable UUID methodId) {
        validateLogin();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

        paymentMethodService.setDefaultMethod(currentUserId, methodId);
        return ResponseEntity.ok(ApiRes.success(null));
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
