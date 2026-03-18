package com.tuttime.user.service.sms;

/**
 * SMS provider that supports delivery status verification via polling.
 * Used for signup and password reset flows to ensure SMS codes are delivered.
 */
public interface SmsDeliveryVerifiableProvider extends SmsProvider {
    
    /**
     * Sends verification code and returns result with request ID for status polling.
     * 
     * @param phoneNumber Phone number in E.164 format
     * @param code Verification code to send
     * @return Send result with requestId for delivery status polling
     * @throws Exception if sending fails
     */
    SmsSendResult sendVerificationCodeWithTracking(String phoneNumber, String code) throws Exception;
    
    /**
     * Checks delivery status of a sent SMS by request ID.
     * 
     * @param requestId Provider-specific request/message ID from send response
     * @return Delivery status
     */
    DeliveryStatus checkDeliveryStatus(String requestId);
    
    /**
     * SMS delivery status.
     */
    enum DeliveryStatus {
        /** Message delivered to recipient */
        DELIVERED,
        /** Message not yet delivered (pending) */
        PENDING,
        /** Message delivery failed */
        FAILED,
        /** Status unknown or provider doesn't support status check */
        UNKNOWN
    }
}
