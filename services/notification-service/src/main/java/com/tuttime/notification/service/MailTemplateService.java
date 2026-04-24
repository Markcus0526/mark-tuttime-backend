package com.tuttime.notification.service;

import java.util.Map;

public interface MailTemplateService {

	String generateHtmlContent(String templateName, Map<String, Object> variables);

}
