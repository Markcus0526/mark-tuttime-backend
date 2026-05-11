package com.tuttime.gateway.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Logback filter to exclude health check logs from Graylog.
 */
public class HealthCheckLogFilter extends Filter<ILoggingEvent> {

	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (event == null) {
			return FilterReply.NEUTRAL;
		}

		String message = event.getFormattedMessage();
		String loggerName = event.getLoggerName();

		// Check logger name first (most reliable indicator)
		if (loggerName != null) {
			String lowerLoggerName = loggerName.toLowerCase();
			// Check for Spring Boot Actuator health-related loggers
			if (lowerLoggerName.contains("org.springframework.boot.actuate.health") ||
					lowerLoggerName.contains("actuate.health") ||
					lowerLoggerName.contains("healthindicator") ||
					lowerLoggerName.contains("healthendpoint")) {
				return FilterReply.DENY;
			}
		}

		// Check message content for health check patterns
		if (message != null) {
			String lowerMessage = message.toLowerCase();
			// Check for actuator health endpoint patterns
			if (lowerMessage.contains("/actuator/health") ||
					lowerMessage.contains("actuator/health") ||
					lowerMessage.contains("healthindicator") ||
					lowerMessage.contains("healthendpoint") ||
					lowerMessage.contains("health endpoint") ||
					// Check for health check probe patterns
					(lowerMessage.contains("health check") &&
							(lowerMessage.contains("probe") || lowerMessage.contains("liveness") || lowerMessage.contains("readiness")))) {
				return FilterReply.DENY;
			}

			// Suppress DEBUG logs from Spring Web framework for actuator endpoints
			if (loggerName != null) {
				String lowerLoggerName = loggerName.toLowerCase();

				// Suppress Zipkin tracing DEBUG logs
				if (lowerLoggerName.contains("exchangefunctions") &&
						event.getLevel().levelInt <= Level.DEBUG.levelInt) {
					if (lowerMessage.contains("zipkin") ||
							lowerMessage.contains("/api/v2/spans") ||
							lowerMessage.contains("http post") ||
							lowerMessage.contains("response 202 accepted")) {
						return FilterReply.DENY;
					}
				}

				// Suppress HttpLogging DEBUG logs for Zipkin/actuator
				if (lowerLoggerName.contains("httplogging") &&
						event.getLevel().levelInt <= Level.DEBUG.levelInt) {
					if (lowerMessage.contains("zipkin") ||
							lowerMessage.contains("/api/v2/spans") ||
							lowerMessage.contains("writing") ||
							lowerMessage.contains("actuator") ||
							lowerMessage.contains("systemhealth")) {
						return FilterReply.DENY;
					}
				}

				// Suppress ResponseEntityResultHandler DEBUG logs for actuator
				if (lowerLoggerName.contains("responseentityresulthandler") &&
						event.getLevel().levelInt <= Level.DEBUG.levelInt) {
					if (lowerMessage.contains("actuator") ||
							lowerMessage.contains("vnd.spring-boot.actuator") ||
							lowerMessage.contains("systemhealth")) {
						return FilterReply.DENY;
					}
				}

				// Suppress HttpWebHandlerAdapter DEBUG logs for actuator endpoints
				if (lowerLoggerName.contains("httpwebhandleradapter") &&
						event.getLevel().levelInt <= Level.DEBUG.levelInt) {
					if (lowerMessage.contains("/actuator/") ||
							lowerMessage.contains("completed")) {
						return FilterReply.DENY;
					}
				}
			}
		}

		return FilterReply.NEUTRAL;
	}
}

