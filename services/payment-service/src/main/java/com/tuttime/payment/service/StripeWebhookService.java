package com.tuttime.payment.service;

public interface StripeWebhookService {

	public void handleWebhook(String payload, String sigHeader);

}
