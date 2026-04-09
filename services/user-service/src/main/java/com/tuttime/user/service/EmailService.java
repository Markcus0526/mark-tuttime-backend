package com.tuttime.user.service;

public interface EmailService {

	void sendVerificationEmail(String toEmail, String code);

	void sendVerificationPasswordEmail(String toEmail, String code, String password);
}