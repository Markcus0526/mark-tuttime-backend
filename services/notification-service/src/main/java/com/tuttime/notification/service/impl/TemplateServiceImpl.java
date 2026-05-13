package com.tuttime.notification.service.impl;

import com.tuttime.notification.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

	private final SpringTemplateEngine templateEngine;

	@Override
	public String generateHtmlContent(String rawContent, Map<String, Object> variables) {
		if (rawContent == null || rawContent.isEmpty()) return "";
		// Use Apache Commons Text or simple regex
		// Example using StringSubstitutor (Apache Commons Text)
		StringSubstitutor sub = new StringSubstitutor(variables, "{{", "}}");
		return sub.replace(rawContent);
	}
}
