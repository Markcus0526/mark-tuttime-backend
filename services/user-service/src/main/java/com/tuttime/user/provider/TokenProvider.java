package com.tuttime.user.provider;

import org.springframework.stereotype.Component;

@Component
public class TokenProvider {

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
