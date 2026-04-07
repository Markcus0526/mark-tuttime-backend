package com.tuttime.user.service.impl;

import com.tuttime.user.config.EmailProperties;
import com.tuttime.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

	private final JavaMailSender mailSender;
	private final EmailProperties emailProperties;

	@Override
	public void sendVerificationEmail(String toEmail, String token) {
		String verificationUrl = "http://173.249.29.61:8080/api/v1/auth/email/verify?token=" + token;
		String subject = "Email Verification";
		String body = "Please verify your email by clicking the link below:\n" + verificationUrl;

		sendSimpleEmail(toEmail, subject, body);
	}

	@Override
	public void sendPasswordResetEmail(String toEmail, String token) {
		String resetUrl = "http://173.249.29.61:8080/api/v1/auth/email/verify?token=" + token;
		String subject = "Password Reset Request";
		String body = "To reset your password, please click the link below:\n" + resetUrl;

		sendSimpleEmail(toEmail, subject, body);
	}

	private void sendSimpleEmail(String toEmail, String subject, String body) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(formatFromAddress());
			message.setTo(toEmail);
			message.setSubject(subject);
			message.setText(body);

			// comment for testing
			// mailSender.send(message);
			log.info("Sent email to {}", toEmail);
		} catch (Exception e) {
			log.error("Failed to send email to {}", toEmail, e);
			throw new RuntimeException("Failed to send email", e);
		}
	}

	private String formatFromAddress() {
		if (emailProperties.getFromName() != null && !emailProperties.getFromName().isBlank()) {
			return String.format("%s <%s>", emailProperties.getFromName(), emailProperties.getFromEmail());
		}
		return emailProperties.getFromEmail();
	}
}