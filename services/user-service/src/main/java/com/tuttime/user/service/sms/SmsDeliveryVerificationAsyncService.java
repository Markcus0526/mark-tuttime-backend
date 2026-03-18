package com.tuttime.user.service.sms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import static com.tuttime.user.service.sms.SmsDeliveryVerifiableProvider.DeliveryStatus;

/**
 * Async service for SMS delivery verification and retry.
 * Polls provider status and retries send if not delivered, without blocking HTTP threads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsDeliveryVerificationAsyncService {

    @Value("${sms.delivery.poll-interval-ms:2000}")
    private long deliveryPollIntervalMs;

    @Value("${sms.delivery.max-poll-attempts:10}")
    private int deliveryMaxPollAttempts;

    @Value("${sms.delivery.max-send-retries:3}")
    private int deliveryMaxSendRetries;

    /**
     * Polls delivery status and retries send in background if not delivered.
     * Called after initial sync send returns a request_id.
     *
     * @param requestId      Provider request ID from initial send
     * @param phoneNumber    Phone number
     * @param code           Verification code (already stored in Redis)
     * @param verifiableProvider Provider supporting delivery verification
     * @param providerName   Provider name for logging
     */
    @Async("smsDeliveryExecutor")
    public void verifyAndRetryDeliveryAsync(
            String requestId,
            String phoneNumber,
            String code,
            SmsDeliveryVerifiableProvider verifiableProvider,
            String providerName) {
        int sendAttempt = 0;
        String currentRequestId = requestId;

        while (sendAttempt <= deliveryMaxSendRetries) {
            for (int pollAttempt = 0; pollAttempt < deliveryMaxPollAttempts; pollAttempt++) {
                try {
                    Thread.sleep(deliveryPollIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("SMS delivery verification interrupted for phone: {}", phoneNumber);
                    return;
                }

                DeliveryStatus status = verifiableProvider.checkDeliveryStatus(currentRequestId);
                if (status == DeliveryStatus.DELIVERED) {
                    log.info("SMS delivery confirmed for phone: {}, request_id: {}", phoneNumber, currentRequestId);
                    return;
                }
                if (status == DeliveryStatus.FAILED) {
                    log.warn("SMS delivery failed for phone: {}, request_id: {}, retrying send (attempt {})",
                            phoneNumber, currentRequestId, sendAttempt + 1);
                    break;
                }
                if (status == DeliveryStatus.UNKNOWN && pollAttempt == 0) {
                    log.debug("SMS status check returned UNKNOWN (endpoint may be unconfigured), assuming delivered");
                    return;
                }
                log.debug("SMS delivery pending for phone: {}, request_id: {}, poll {}/{}",
                        phoneNumber, currentRequestId, pollAttempt + 1, deliveryMaxPollAttempts);
            }

            sendAttempt++;
            if (sendAttempt > deliveryMaxSendRetries) {
                log.warn("SMS delivery verification exhausted retries for phone: {} after {} send attempts",
                        phoneNumber, deliveryMaxSendRetries + 1);
                return;
            }

            try {
                SmsSendResult result = verifiableProvider.sendVerificationCodeWithTracking(phoneNumber, code);
                currentRequestId = result != null && result.getRequestId() != null ? result.getRequestId() : currentRequestId;
                log.info("SMS delivery retry sent for phone: {} (attempt {}/{})", phoneNumber, sendAttempt + 1, deliveryMaxSendRetries + 1);
            } catch (Exception e) {
                if (isRateLimitError(e)) {
                    log.warn("SMS provider rate limit (429) for phone: {}, stopping retry attempts. User can request a new code after timeout.", phoneNumber);
                    return;
                } else {
                    log.error("SMS delivery retry failed for phone: {}: {}", phoneNumber, e.getMessage());
                    return;
                }
            }
        }
    }

    private boolean isRateLimitError(Throwable e) {
        if (e.getMessage() != null && e.getMessage().contains("SMS_RATE_LIMIT_EXCEEDED")) {
            return true;
        }
        Throwable cause = e.getCause();
        if (cause instanceof HttpClientErrorException httpEx) {
            return httpEx.getStatusCode() != null && httpEx.getStatusCode().value() == 429;
        }
        return false;
    }
}
