package com.tuttime.notification.config;

import org.springframework.context.annotation.Bean;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

public class TemplateConfig {

	@Bean
	public StringTemplateResolver stringTemplateResolver() {
		StringTemplateResolver resolver = new StringTemplateResolver();
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(true);
		return resolver;
	}

}
