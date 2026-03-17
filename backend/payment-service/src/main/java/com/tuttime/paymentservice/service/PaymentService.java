package com.tuttime.paymentservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import com.tuttime.paymentservice.dto.PaymentMethodRequest;
import com.tuttime.paymentservice.dto.PaymentRequest;
import com.tuttime.paymentservice.dto.PaymentResponse;
import com.tuttime.paymentservice.dto.RefundRequest;
import com.tuttime.paymentservice.entity.Payment;
import com.tuttime.paymentservice.repository.PaymentRepository;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public PaymentResponse processPayment(PaymentRequest request, PaymentMethodRequest method) {
    BigDecimal payAmount = "FULL_PREPAY".equals(method.getMethod()) ? request.getAmount()
        : request.getAmount().multiply(method.getDepositPercentage());
    Payment payment = Payment.builder()
        .orderId(request.getOrderId())
        .amount(payAmount)
        .status("HELD")
        .transactionId(UUID.randomUUID().toString())
        .build();
    payment = paymentRepository.save(payment);

    // Kafka to order
    kafkaTemplate.send("payment-events", "payment.held", payment.getId());

    return mapToResponse(payment);
  }

  public PaymentResponse refund(Long paymentId, RefundRequest request, RefundPolicyService policy) {
    Payment payment = paymentRepository.findById(paymentId).orElseThrow();
    // Apply policy
    BigDecimal refundAmount = request.getAmount();
    // Stub: N min full/partial
    payment.setStatus("REFUNDED");
    payment = paymentRepository.save(payment);

    kafkaTemplate.send("payment-events", "payment.refunded", paymentId);

    return mapToResponse(payment);
  }

  private PaymentResponse mapToResponse(Payment payment) {
    PaymentResponse response = new PaymentResponse();
    response.setId(payment.getId());
    response.setStatus(payment.getStatus());
    response.setTransactionId(payment.getTransactionId());
    return response;
  }

}
