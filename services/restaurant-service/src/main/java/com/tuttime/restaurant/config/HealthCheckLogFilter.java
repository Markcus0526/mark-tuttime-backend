package com.tuttime.restaurant.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logback filter to exclude health check logs from Graylog (reduces noise).
 */
public class HealthCheckLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null) {
            return FilterReply.NEUTRAL;
        }

        String message = event.getFormattedMessage();
        String loggerName = event.getLoggerName();

        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("/actuator/health") ||
                    lowerMessage.contains("/health") ||
                    lowerMessage.contains("healthindicator") ||
                    lowerMessage.contains("health check") ||
                    lowerMessage.contains("healthendpoint") ||
                    lowerMessage.contains("health endpoint")) {
                return FilterReply.DENY;
            }
        }

        if (loggerName != null) {
            String lowerLoggerName = loggerName.toLowerCase();
            if (lowerLoggerName.contains("actuate.health") ||
                    lowerLoggerName.contains("healthindicator") ||
                    lowerLoggerName.contains("healthendpoint")) {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}

