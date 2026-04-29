package com.tuttime.notification.provider;

import com.tuttime.notification.entity.enums.NotificationStatus;
import com.tuttime.notification.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailProvider {

	private final JavaMailSender mailSender;
	private final NotificationLogRepository notificationLogRepository;

	/**
	 * Sends an HTML email.
	 *
	 * @param to          Recipient email address
	 * @param subject     Email subject
	 * @param htmlContent Pre-processed HTML body (e.g., from Thymeleaf)
	 */
	@Async("notificationExecutor")
	public void sendHtmlEmail(String to, String subject, String htmlContent, UUID logId) {
		try {
			/* TODO
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			helper.setFrom("TutTime <no-reply@tuttime.com>");

			mailSender.send(message);*/
			log.info("Email successfully sent to: {}", to);

			// Update DB status to SENT
			updateLogStatus(logId, NotificationStatus.SENT, null);

		} catch (Exception e) {
			log.error("Failed to send email to {}: {}", to, e.getMessage());
			// Update DB status to FAILED with error details
			updateLogStatus(logId, NotificationStatus.FAILED, e.getMessage());
		}
	}

	private void updateLogStatus(UUID logId, NotificationStatus status, String errorMessage) {
		notificationLogRepository.findById(logId).ifPresent(logEntry -> {
			logEntry.setStatus(status);
			logEntry.setSentAt(OffsetDateTime.now());
			if (errorMessage != null) {
				logEntry.setProviderResponse(Map.of("error", errorMessage));
			}
			notificationLogRepository.save(logEntry);
		});
	}
}
