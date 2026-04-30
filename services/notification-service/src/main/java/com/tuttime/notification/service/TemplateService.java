package com.tuttime.notification.service;

import java.util.Map;

public interface TemplateService {

	String generateHtmlContent(String templateName, Map<String, Object> variables);

}
