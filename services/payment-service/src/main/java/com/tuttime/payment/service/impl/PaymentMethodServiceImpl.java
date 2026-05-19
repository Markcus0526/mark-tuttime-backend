package com.tuttime.payment.service.impl;

import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.payment.dto.PaymentMethodDto;
import com.tuttime.payment.entity.UserPaymentMethod;
import com.tuttime.payment.provider.YooKassaClient;
import com.tuttime.payment.repository.PaymentMethodRepository;
import com.tuttime.payment.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final YooKassaClient yooKassaClient;

    @Transactional
    public String initializeCardBinding(UUID userId) {
        // 1. Create a 1.00 RUB "Verification" payment
        // The shopId and secretKey from YooKassaClient are used for authentication
        String confirmationUrl = yooKassaClient.createBindingPayment(userId);

        log.info("Initiated card binding for user {}. Redirecting to YooKassa.", userId);
        return confirmationUrl;
    }

    /**
     * Method 1 & 2: Save token logic (Called by Webhook)
     * Handles persisting the YooKassa payment_method object to our DB.
     */
    @Transactional
    public void savePaymentMethod(UUID userId, PaymentMethodDto.YooKassaPaymentMethod yooMethod) {
        if (!yooMethod.saved()) {
            log.warn("Payment method not saved by YooKassa for user: {}", userId);
            return;
        }

        // 1. If this is the first card, make it default
        boolean isFirstCard = !paymentMethodRepository.existsByUserId(userId);

        UserPaymentMethod entity = UserPaymentMethod.builder()
                .userId(userId)
                .yookassaPaymentMethodId(yooMethod.id())
                .cardType(yooMethod.card().cardType())
                .last4(yooMethod.card().last4())
                .expiryMonth(yooMethod.card().expiryMonth())
                .expiryYear(yooMethod.card().expiryYear())
                .defaultMethod(isFirstCard)
                .active(true)
                .build();

        paymentMethodRepository.save(entity);
        log.info("Saved new payment method {} for user {}", yooMethod.id(), userId);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodDto.Response> getUserPaymentMethods(UUID userId) {
        return paymentMethodRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removePaymentMethod(UUID userId, UUID methodId) {
        UserPaymentMethod method = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new InvalidRequestException("Payment method not found"));

        if (!method.getUserId().equals(userId)) {
            throw new InvalidRequestException("Unauthorized removal attempt");
        }

        // Soft delete
        method.setActive(false);
        paymentMethodRepository.save(method);
    }

    @Transactional
    public void setDefaultMethod(UUID userId, UUID methodId) {
        // 1. Reset all others
        paymentMethodRepository.resetDefaultFlags(userId);

        // 2. Set new default
        UserPaymentMethod method = paymentMethodRepository.findById(methodId)
                .orElseThrow(() -> new InvalidRequestException("Payment method not found"));

        method.setDefaultMethod(true);
        paymentMethodRepository.save(method);
    }

    private PaymentMethodDto.Response mapToResponse(UserPaymentMethod entity) {
        return PaymentMethodDto.Response.builder()
                .id(entity.getId())
                .cardType(entity.getCardType())
                .last4(entity.getLast4())
                .expiryMonth(entity.getExpiryMonth())
                .expiryYear(entity.getExpiryYear())
                .isDefault(entity.getDefaultMethod())
                .linkedAt(entity.getCreatedAt())
                .build();
    }
}
