package com.tuttime.payment.controller;

import com.tuttime.payment.dto.PaymentMethodRequest;
import com.tuttime.payment.dto.PaymentRequest;
import com.tuttime.payment.dto.PaymentResponse;
import com.tuttime.payment.dto.RefundRequest;
import com.tuttime.payment.service.PaymentService;
import com.tuttime.payment.service.RefundPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

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
