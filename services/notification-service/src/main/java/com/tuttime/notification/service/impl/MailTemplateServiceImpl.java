package com.tuttime.notification.service.impl;

import com.tuttime.notification.service.MailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class MailTemplateServiceImpl implements MailTemplateService {

	private final SpringTemplateEngine templateEngine;

	@Override
	public String generateHtmlContent(String rawContent, Map<String, Object> variables) {
		if (rawContent == null || rawContent.isEmpty()) {
			return "";
		}

		// 1. First, handle the {{placeholder}} to [[${variable}]] conversion
		// so Thymeleaf can understand it.
		String thymeleafCompatibleContent = convertToThymeleafSyntax(rawContent);

		// 2. Prepare the Thymeleaf context
		Context context = new Context();
		context.setVariables(variables);

		// 3. Process the string as a template
		return templateEngine.process(thymeleafCompatibleContent, context);
	}

	/**
	 * Converts "Hello {{userName}}" to "Hello [[${userName}]]"
	 * This allows your admins to use simple {{}} syntax in the UI,
	 * but leverages the power of Thymeleaf for rendering.
	 */
	private String convertToThymeleafSyntax(String content) {
		return content.replaceAll("\\{\\{", "[[\\${").replaceAll("\\}\\}", "}]]");
	}
}
