package com.tuttime.payment.service;

import com.tuttime.payment.entity.PaymentAuditLog;

import java.util.List;

public interface PaymentAuditLogService {

	PaymentAuditLog addAuditLog(Long paymentId, String eventType, String eventData);

	List<PaymentAuditLog> getLogsByPaymentId(Long paymentId);

	List<PaymentAuditLog> getLogsByEventType(String eventType);
}
