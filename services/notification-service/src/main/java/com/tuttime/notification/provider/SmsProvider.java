package com.tuttime.notification.provider;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.tuttime.common.exception.InvalidRequestException;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsProvider {

	private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();

	@Value("${twilio.account.sid}")
	private String accountSid;

	@Value("${twilio.auth.token}")
	private String authToken;

	@Value("${twilio.from.number}")
	private String fromNumber;

	@PostConstruct
	public void init() {
		Twilio.init(accountSid, authToken);
	}

	/**
	 * Local validation using Google's libphonenumber.
	 * Use "ZZ" for international/E.164 numbers or a specific country code (e.g., "US").
	 */
	public boolean isLocallyValid(String rawNumber) {
		try {
			Phonenumber.PhoneNumber numberProto = phoneUtil.parse(rawNumber, "ZZ");
			return phoneUtil.isValidNumber(numberProto);
		} catch (NumberParseException e) {
			log.warn("Phone number parsing failed: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * Formats number to E.164 (+1234567890) for Twilio.
	 */
	public String formatToE164(String rawNumber) {
		try {
			Phonenumber.PhoneNumber numberProto = phoneUtil.parse(rawNumber, "ZZ");
			return phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
		} catch (NumberParseException e) {
			return rawNumber; // Fallback to raw if parsing fails
		}
	}

	public void send(String phoneNumber, String content) {
		String formattedNumber = formatToE164(phoneNumber);
		Message.creator(
				new com.twilio.type.PhoneNumber(formattedNumber),
				new com.twilio.type.PhoneNumber(fromNumber),
				content
		).create();
	}

	public boolean isTwilioValid(String rawNumber) {
		// Fast-fail if structurally invalid
		if (!isLocallyValid(rawNumber)) return false;

		try {
			com.twilio.rest.lookups.v2.PhoneNumber lookup =
					com.twilio.rest.lookups.v2.PhoneNumber.fetcher(rawNumber).fetch();
			return lookup.getValid();
		} catch (com.twilio.exception.ApiException e) {
			if (e.getStatusCode() == 404) return false;
			throw new InvalidRequestException("Twilio service error during lookup");
		}
	}
}
