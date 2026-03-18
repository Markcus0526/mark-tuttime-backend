package com.tuttime.user.config;

import com.tuttime.user.service.sms.GreenSmsProvider;
import com.tuttime.user.service.sms.SmsBalanceResult;
import com.tuttime.user.service.sms.TwilioSmsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Health indicator for SMS provider balances.
 * Returns UP when balance OK, WARNING when low, DOWN when critical.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sms.balance.health-enabled", havingValue = "true", matchIfMissing = false)
public class SmsBalanceHealthIndicator implements HealthIndicator {

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

    @Override
    public Health health() {
        List<String> downReasons = new ArrayList<>();
        List<String> warningReasons = new ArrayList<>();
        List<Object> details = new ArrayList<>();

        checkProvider(greenSmsProvider.getBalance(), greenWarningThreshold, greenCriticalThreshold, greenCostPerSms,
                downReasons, warningReasons, details);
        checkProvider(twilioSmsProvider.getBalance(), twilioWarningThreshold, twilioCriticalThreshold, twilioCostPerSms,
                downReasons, warningReasons, details);

        Health.Builder builder = Health.up();
        if (!downReasons.isEmpty()) {
            builder = Health.down().withDetail("reasons", downReasons);
        } else if (!warningReasons.isEmpty()) {
            builder = Health.status("WARNING").withDetail("reasons", warningReasons);
        }
        return builder.withDetails(java.util.Map.of("providers", details)).build();
    }

    private void checkProvider(SmsBalanceResult result, double warningThreshold, double criticalThreshold,
                               double costPerSms, List<String> downReasons, List<String> warningReasons,
                               List<Object> details) {
        if (result == null) {
            return;
        }
        double balance = result.getBalance();
        String provider = result.getProviderName();
        String currency = result.getCurrency() != null ? result.getCurrency() : "";
        long estimatedSends = costPerSms > 0 ? (long) (balance / costPerSms) : 0;

        var providerDetail = new java.util.LinkedHashMap<String, Object>();
        providerDetail.put("provider", provider);
        providerDetail.put("balance", balance);
        providerDetail.put("currency", currency);
        providerDetail.put("estimatedSendsRemaining", estimatedSends);
        details.add(providerDetail);

        if (balance < criticalThreshold) {
            downReasons.add(provider + " balance CRITICAL: " + balance + " " + currency);
        } else if (balance < warningThreshold) {
            warningReasons.add(provider + " balance low: " + balance + " " + currency);
        }
    }
}
