package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.user.config.SmsProperties;
import com.tuttime.user.service.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsServiceImpl implements SmsService {

	private final SmsProperties smsProperties;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@PostConstruct
	public void init() {
		if ("twilio".equalsIgnoreCase(smsProperties.getProvider())) {
			Twilio.init(smsProperties.getTwilio().getAccountSid(), smsProperties.getTwilio().getAuthToken());
			log.info("Twilio initialized with account SID {}", smsProperties.getTwilio().getAccountSid());
		} else if ("greensms".equalsIgnoreCase(smsProperties.getProvider())) {
			log.info("Using GreenSMS provider");
		} else if ("log".equalsIgnoreCase(smsProperties.getProvider())) {
			log.info("SMS provider set to 'log' - SMS messages will only be logged");
		} else {
			log.warn("Unknown SMS provider configured: {}", smsProperties.getProvider());
		}
	}

	@Override
	public void sendVerificationCode(String phoneNumber, String code) {
		sendSms(phoneNumber, "Your verification code is: " + code);
	}

	@Override
	public void sendPasswordResetCode(String phoneNumber, String code) {
		sendSms(phoneNumber, "Your password reset code is: " + code);
	}

	private void sendSms(String toPhoneNumber, String message) {
		switch (smsProperties.getProvider().toLowerCase()) {
			case "twilio" -> sendViaTwilio(toPhoneNumber, message);
			case "greensms" -> sendViaGreenSms(toPhoneNumber, message);
			case "log" -> log.info("[SMS LOG] To: {}, Message: {}", toPhoneNumber, message);
			default -> {
				log.error("No valid SMS provider configured. SMS not sent.");
				throw new MethodArgumentNotValidException("No valid SMS provider configured.");
			}
		}
	}

	private void sendViaTwilio(String toPhoneNumber, String message) {
		try {
			Message.creator(
					new PhoneNumber(toPhoneNumber),
					new PhoneNumber(smsProperties.getFromNumber()),
					message
			).create();
			log.info("Sent SMS via Twilio to {}", toPhoneNumber);
		} catch (Exception e) {
			log.error("Failed to send SMS via Twilio", e);
			throw new BusinessException("Failed to send SMS via Twilio", e);
		}
	}

	private void sendViaGreenSms(String toPhoneNumber, String message) {
		try {
			String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
			String encodedTo = URLEncoder.encode(toPhoneNumber, StandardCharsets.UTF_8);
			String encodedFrom = URLEncoder.encode(smsProperties.getFromNumber(), StandardCharsets.UTF_8);

			String url = String.format("%s/sms/send?from=%s&amp;to=%s&amp;text=%s",
					smsProperties.getGreen().getApiUrl(),
					encodedFrom,
					encodedTo,
					encodedMessage);

			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Authorization", "Bearer " + smsProperties.getGreen().getJwtToken())
					.GET()
					.build();

			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 200) {
				log.info("Sent SMS via GreenSMS to {}", toPhoneNumber);
			} else {
				log.error("Failed to send SMS via GreenSMS. Status: {}, Response: {}", response.statusCode(), response.body());
				throw new BusinessException("Failed to send SMS via GreenSMS");
			}
		} catch (Exception e) {
			log.error("Exception while sending SMS via GreenSMS", e);
			throw new BusinessException("Failed to send SMS via GreenSMS", e);
		}
	}
}