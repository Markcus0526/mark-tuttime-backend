package com.tuttime.notification.service;

import com.tuttime.notification.dto.AdminNotificationDto;
import com.tuttime.notification.entity.NotificationTemplate;

import java.util.List;
import java.util.Map;

public interface AdminNotificationService {

	NotificationTemplate createTemplate(AdminNotificationDto.TemplateRequest dto);

	NotificationTemplate updateTemplate(Integer id, AdminNotificationDto.TemplateRequest dto);

	String renderPreview(String content, Map<String, Object> params);

	List<NotificationTemplate> getAllTemplates();
}
