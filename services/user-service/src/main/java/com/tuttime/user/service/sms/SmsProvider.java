package com.tuttime.user.service.sms;

/**
 * SMS Provider Interface
 * 
 * <p>Interface for different SMS verification providers.
 * Implementations handle sending verification codes via different channels.
 */
public interface SmsProvider {
    
    /**
     * Sends a verification code to the specified phone number.
     * 
     * @param phoneNumber Phone number in E.164 format (e.g., +1234567890)
     * @param code Verification code to send
     * @throws Exception if sending fails
     */
    void sendVerificationCode(String phoneNumber, String code) throws Exception;
    
    /**
     * Gets the provider name for logging/identification.
     * 
     * @return Provider name
     */
    String getProviderName();
}

