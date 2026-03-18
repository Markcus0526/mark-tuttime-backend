package com.tuttime.user.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/** Logback filter to exclude health check logs from Graylog. */
public class HealthCheckLogFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event == null) {
            return FilterReply.NEUTRAL;
        }

        String message = event.getFormattedMessage();
        String loggerName = event.getLoggerName();

        if (loggerName != null) {
            String lowerLoggerName = loggerName.toLowerCase();
            if (lowerLoggerName.contains("org.springframework.boot.actuate.health") ||
                lowerLoggerName.contains("actuate.health") ||
                lowerLoggerName.contains("healthindicator") ||
                lowerLoggerName.contains("healthendpoint")) {
                return FilterReply.DENY;
            }
        }

        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("/actuator/health") ||
                lowerMessage.contains("actuator/health") ||
                lowerMessage.contains("healthindicator") ||
                lowerMessage.contains("healthendpoint") ||
                lowerMessage.contains("health endpoint") ||
                (lowerMessage.contains("health check") && 
                 (lowerMessage.contains("probe") || lowerMessage.contains("liveness") || lowerMessage.contains("readiness")))) {
                return FilterReply.DENY;
            }
        }

        return FilterReply.NEUTRAL;
    }
}

