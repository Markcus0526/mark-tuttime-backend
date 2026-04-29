package com.tuttime.notification.service.impl;

import com.tuttime.notification.dto.AdminNotificationDto;
import com.tuttime.notification.entity.NotificationTemplate;
import com.tuttime.notification.repository.NotificationTemplateRepository;
import com.tuttime.notification.service.AdminNotificationService;
import com.tuttime.notification.service.MailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

	private final NotificationTemplateRepository templateRepository;
	private final MailTemplateService templateEngine;

	@Transactional
	public NotificationTemplate createTemplate(AdminNotificationDto.TemplateRequest dto) {
		if (templateRepository.existsByTemplateKeyAndChannelAndLanguage(dto.templateKey(), dto.channel(), dto.language())) {
			throw new RuntimeException("Conflict: Template already exists for this configuration.");
		}

		NotificationTemplate template = NotificationTemplate.builder()
				.templateKey(dto.templateKey())
				.channel(dto.channel())
				.language(dto.language())
				.title(dto.title())
				.content(dto.content())
				.active(dto.active())
				.build();

		return templateRepository.save(template);
	}

	@Transactional
	public NotificationTemplate updateTemplate(Integer id, AdminNotificationDto.TemplateRequest dto) {
		NotificationTemplate template = templateRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Template not found"));

		template.setTitle(dto.title());
		template.setContent(dto.content());
		template.setActive(dto.active());

		return templateRepository.save(template);
	}

	public String renderPreview(String rawContent, Map<String, Object> mockParams) {
		// If mockParams is empty, provide some default test values
		if (mockParams == null || mockParams.isEmpty()) {
			mockParams = Map.of(
					"userName", "John Doe",
					"code", "123456",
					"restaurantName", "TutTime Bistro",
					"bookingTime", "19:00",
					"amount", "250.00"
			);
		}
		// Call the MailTemplateService we just built
		try {
			return templateEngine.generateHtmlContent(rawContent, mockParams);
		} catch (Exception e) {
			return "<b>Error in template syntax:</b> " + e.getMessage();
		}
	}

	public List<NotificationTemplate> getAllTemplates() {
		return templateRepository.findAll();
	}
}
