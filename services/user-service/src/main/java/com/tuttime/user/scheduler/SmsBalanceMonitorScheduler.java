package com.tuttime.user.scheduler;

import com.tuttime.user.service.sms.GreenSmsProvider;
import com.tuttime.user.service.sms.SmsBalanceResult;
import com.tuttime.user.service.sms.TwilioSmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that checks SMS provider balances and logs warnings when low.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sms.balance.monitoring-enabled", havingValue = "true", matchIfMissing = false)
public class SmsBalanceMonitorScheduler {

    private final GreenSmsProvider greenSmsProvider;
    private final TwilioSmsProvider twilioSmsProvider;

    @Value("${sms.balance.green.warning-threshold:500}")
    private double greenWarningThreshold;

    @Value("${sms.balance.green.critical-threshold:100}")
    private double greenCriticalThreshold;

    @Value("${sms.balance.green.cost-per-sms:2.5}")
    private double greenCostPerSms;

    @Value("${sms.balance.twilio.warning-threshold:50}")
    private double twilioWarningThreshold;

    @Value("${sms.balance.twilio.critical-threshold:10}")
    private double twilioCriticalThreshold;

    @Value("${sms.balance.twilio.cost-per-sms:0.05}")
    private double twilioCostPerSms;

    @Scheduled(fixedRateString = "${sms.balance.check-interval-ms:1800000}")
    public void checkSmsBalances() {
        checkProviderBalance(greenSmsProvider.getBalance(), greenWarningThreshold, greenCriticalThreshold, greenCostPerSms);
        checkProviderBalance(twilioSmsProvider.getBalance(), twilioWarningThreshold, twilioCriticalThreshold, twilioCostPerSms);
    }

    private void checkProviderBalance(SmsBalanceResult result, double warningThreshold, double criticalThreshold, double costPerSms) {
        if (result == null) {
            return;
        }
        double balance = result.getBalance();
        String provider = result.getProviderName();
        String currency = result.getCurrency() != null ? result.getCurrency() : "";
        long estimatedSends = costPerSms > 0 ? (long) (balance / costPerSms) : 0;

        if (balance < criticalThreshold) {
            log.error("SMS provider {} balance CRITICAL: {} {}, estimated {} sends remaining (threshold: {})",
                    provider, balance, currency, estimatedSends, criticalThreshold);
        } else if (balance < warningThreshold) {
            log.warn("SMS provider {} balance low: {} {}, estimated {} sends remaining (threshold: {})",
                    provider, balance, currency, estimatedSends, warningThreshold);
        } else {
            log.debug("SMS provider {} balance OK: {} {}, estimated {} sends remaining",
                    provider, balance, currency, estimatedSends);
        }
    }
}
