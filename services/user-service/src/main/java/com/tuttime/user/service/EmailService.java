package com.tuttime.user.service;

public interface EmailService {

	void sendVerificationEmail(String toEmail, String token);

	void sendPasswordResetEmail(String toEmail, String token);
}