package com.tuttime.user.service;

public interface SmsService {

	void sendVerificationCode(String phoneNumber, String code);

	void sendPasswordResetCode(String phoneNumber, String code);

}
