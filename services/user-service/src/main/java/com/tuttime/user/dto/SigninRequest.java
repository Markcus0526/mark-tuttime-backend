package com.tuttime.user.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class SigninRequest {
	private String phoneNumber;
	private String smsCode;

	@Email
	private String email;

	private String password;
}