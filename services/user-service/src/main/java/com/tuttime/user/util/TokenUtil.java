package com.tuttime.user.util;

import com.tuttime.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TokenUtil {

	public String generateRandomCode() {
		// Generate numeric code (e.g., 6-digit)
		return "123456";
	}

	public String generateRandomPassword() {
		// Generate and persist password reset token
		//return UUID.randomUUID().toString();
		return "12345678";
	}
}
