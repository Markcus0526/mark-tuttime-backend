package com.tuttime.paymentservice.controller;

import java.math.BigDecimal;

import com.tuttime.paymentservice.dto.PaymentMethodRequest;
import com.tuttime.paymentservice.dto.PaymentRequest;
import com.tuttime.paymentservice.dto.PaymentResponse;
import com.tuttime.paymentservice.dto.RefundRequest;
import com.tuttime.paymentservice.service.PaymentService;
import com.tuttime.paymentservice.service.RefundPolicyService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final RefundPolicyService refundPolicyService;

  @PostMapping
  public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request,
      @RequestParam String method, @RequestParam(defaultValue = "0.5") BigDecimal depositPct) {
    PaymentMethodRequest methodReq = new PaymentMethodRequest();
    methodReq.setMethod(method);
    methodReq.setDepositPercentage(depositPct);
    return ResponseEntity.ok(paymentService.processPayment(request, methodReq));
  }

  @PostMapping("/{id}/refund")
  public ResponseEntity<PaymentResponse> refund(@PathVariable Long id, @RequestBody RefundRequest request,
      RefundPolicyService policy) {
    return ResponseEntity.ok(paymentService.refund(id, request, policy));
  }

}
