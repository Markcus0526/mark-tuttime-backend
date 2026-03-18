package com.tuttime.user.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP Email Provider
 * 
 * <p>Sends verification emails via SMTP.
 */
@Slf4j
@Component
public class SmtpEmailProvider {
    
    private final JavaMailSender mailSender;
    
    @Value("${email.smtp.from-email:noreply@aparttoken.com}")
    private String fromEmail;
    
    @Value("${email.smtp.from-name:APART TOKEN}")
    private String fromName;
    
    public SmtpEmailProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Sends a verification code email via SMTP.
     * 
     * @param toEmail Recipient email address
     * @param code 4-digit verification code
     * @throws Exception if sending fails
     */
    public void sendVerificationCode(String toEmail, String code) throws Exception {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Set from address with name
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(fromName + " - Email Verification Code");
            
            // Create email content
            String htmlContent = buildVerificationEmailHtml(code);
            String textContent = buildVerificationEmailText(code);
            
            // Set both HTML and plain text content
            helper.setText(textContent, htmlContent);
            
            log.debug("Sending email via SMTP - From: {} ({}), To: {}", fromEmail, fromName, toEmail);
            
            mailSender.send(message);
            
            log.info("Verification email sent via SMTP to: {}, code: {}", toEmail, code);
        } catch (MessagingException e) {
            log.error("Failed to send verification email via SMTP to {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send verification email via SMTP: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error sending verification email via SMTP to {}: {}", toEmail, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Builds HTML content for verification email.
     */
    private String buildVerificationEmailHtml(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Email Verification</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #f4f4f4; padding: 20px; border-radius: 5px;">
                    <h2 style="color: #333; margin-top: 0;">Email Verification</h2>
                    <p>Please use the following verification code to verify your email address:</p>
                    <div style="background-color: #fff; padding: 20px; border-radius: 5px; text-align: center; margin: 20px 0;">
                        <h1 style="color: #007bff; font-size: 32px; letter-spacing: 5px; margin: 0;">%s</h1>
                    </div>
                    <p>This code will expire in 10 minutes.</p>
                    <p>If you didn't request this verification code, please ignore this email.</p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #666; font-size: 12px;">This is an automated message, please do not reply.</p>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
    
    /**
     * Builds plain text content for verification email.
     */
    private String buildVerificationEmailText(String code) {
        return """
            Email Verification
            
            Please use the following verification code to verify your email address:
            
            %s
            
            This code will expire in 10 minutes.
            
            If you didn't request this verification code, please ignore this email.
            
            This is an automated message, please do not reply.
            """.formatted(code);
    }
}

