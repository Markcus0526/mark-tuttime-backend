package com.tuttime.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

@Configuration
public class ThymeleafConfig {

	@Bean
	public SpringTemplateEngine templateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();

		// Resolver for raw strings from DB
		StringTemplateResolver stringResolver = new StringTemplateResolver();
		stringResolver.setTemplateMode(TemplateMode.HTML);
		stringResolver.setCacheable(false); // Usually false for dynamic DB templates

		engine.setTemplateResolver(stringResolver);
		return engine;
	}
}