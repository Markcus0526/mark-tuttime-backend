package com.tuttime.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "email.smtp")
public class EmailProperties {
	private String fromEmail;
	private String fromName;
}