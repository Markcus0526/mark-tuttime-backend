package com.tuttime.payment.service;

import com.tuttime.payment.dto.PaymentMethodDto;

import java.util.List;
import java.util.UUID;

public interface PaymentMethodService {

	String initializeCardBinding(UUID userId);

	void savePaymentMethod(UUID userId, PaymentMethodDto.YooKassaPaymentMethod yooMethod);

	List<PaymentMethodDto.Response> getUserPaymentMethods(UUID userId);

	void removePaymentMethod(UUID userId, UUID methodId);

	void setDefaultMethod(UUID userId, UUID methodId);
}
