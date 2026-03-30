package com.tuttime.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

	private String provider;
	private String apiKey;
	private String apiSecret;
	private String fromNumber;
	private int codeLength;
	private int maxAttemptsPerHour;

	private GreenProperties green = new GreenProperties();
	private TwilioProperties twilio = new TwilioProperties();

	@Data
	public static class GreenProperties {
		private String apiUrl;
		private String jwtToken;
		private String flashcallEndpoint;
		private String statusEndpoint;
	}

	@Data
	public static class TwilioProperties {
		private String verifyApiUrl;
		private String apiUrl;
		private String accountSid;
		private String authToken;
		private String serviceSid;
	}
}