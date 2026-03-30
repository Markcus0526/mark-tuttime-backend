package com.tuttime.user.service;

import com.tuttime.user.dto.SigninRequest;
import com.tuttime.user.entity.*;
import com.tuttime.user.enums.AuthProvider;
import com.tuttime.user.enums.UserStatus;
import com.tuttime.user.repository.*;
import com.tuttime.user.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final UserAuthMethodRepository userAuthMethodRepository;
	private final PhoneVerificationCodeRepository phoneVerificationCodeRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final EmailVerificationTokenRepository emailVerificationTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	private final Random random = new Random();

	@Transactional
	public String authenticate(SigninRequest request) {
		if (request.getPhoneNumber() != null && request.getSmsCode() != null) {
			// Phone SMS login flow
			PhoneVerificationCode code = phoneVerificationCodeRepository.findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(request.getPhoneNumber())
					.orElseThrow(() -> new EntityNotFoundException("No active SMS code found"));
			if (!code.getCode().equals(request.getSmsCode()) || code.getExpiresAt().isBefore(Instant.now())) {
				throw new IllegalArgumentException("Invalid or expired SMS code");
			}
			code.setUsed(true);
			phoneVerificationCodeRepository.save(code);

			User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
					.orElseThrow(() -> new EntityNotFoundException("User not found"));

			return jwtUtil.generateToken(user.getId());

		} else if (request.getEmail() != null && request.getPassword() != null) {
			// Email/password login flow
			User user = userRepository.findByEmail(request.getEmail())
					.orElseThrow(() -> new EntityNotFoundException("User not found"));

			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
					.orElseThrow(() -> new EntityNotFoundException("Authentication method not found"));

			if (!passwordEncoder.matches(request.getPassword(), authMethod.getCredential())) {
				throw new IllegalArgumentException("Invalid password");
			}

			return jwtUtil.generateToken(user.getId());

		} else {
			throw new IllegalArgumentException("Invalid signin request");
		}
	}

	@Transactional
	public void sendSmsVerificationCode(String phoneNumber) {
		// Generate random 6-digit code
		int codeInt = 100000 + random.nextInt(900000);
		String codeStr = String.valueOf(codeInt);

		PhoneVerificationCode code = PhoneVerificationCode.builder()
				.phoneNumber(phoneNumber)
				.code(codeStr)
				.expiresAt(Instant.now().plusSeconds(300)) // 5 minutes expiry
				.used(false)
				.build();
		phoneVerificationCodeRepository.save(code);

		// TODO: integrate with SMS provider to send 'codeStr' to phoneNumber
		System.out.printf("Sending SMS code %s to %s%n", codeStr, phoneNumber);
	}

	@Transactional
	public void verifySmsCode(String phoneNumber, String code) {
		PhoneVerificationCode phoneCode = phoneVerificationCodeRepository.findTopByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(phoneNumber)
				.orElseThrow(() -> new EntityNotFoundException("No active SMS code found"));

		if (!phoneCode.getCode().equals(code) || phoneCode.getExpiresAt().isBefore(Instant.now())) {
			throw new IllegalArgumentException("Invalid or expired SMS code");
		}
		phoneCode.setUsed(true);
		phoneVerificationCodeRepository.save(phoneCode);

		// Could mark user's phone-verified status here if tracked separately
	}

	@Transactional
	public void sendPasswordResetEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		String token = UUID.randomUUID().toString();

		PasswordResetToken resetToken = PasswordResetToken.builder()
				.user(user)
				.token(token)
				.expiresAt(Instant.now().plusSeconds(3600)) // 1 hour expiry
				.used(false)
				.build();

		passwordResetTokenRepository.save(resetToken);

		// TODO: send email with reset link containing token
		System.out.printf("Sending password reset email to %s with token %s%n", email, token);
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {
		PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(token)
				.orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

		if (resetToken.getExpiresAt().isBefore(Instant.now())) {
			throw new IllegalArgumentException("Expired token");
		}

		User user = resetToken.getUser();

		UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
				.orElseThrow(() -> new IllegalStateException("Email/password auth method not found"));

		authMethod.setCredential(passwordEncoder.encode(newPassword));
		userAuthMethodRepository.save(authMethod);

		resetToken.setUsed(true);
		passwordResetTokenRepository.save(resetToken);
	}

	@Transactional
	public void sendEmailVerificationLink(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		String token = UUID.randomUUID().toString();

		EmailVerificationToken evToken = EmailVerificationToken.builder()
				.user(user)
				.token(token)
				.expiresAt(Instant.now().plusSeconds(86400)) // 24 hours
				.used(false)
				.build();

		emailVerificationTokenRepository.save(evToken);

		// TODO: send email with token link
		System.out.printf("Sending email verification to %s with token %s%n", email, token);
	}

	@Transactional
	public void verifyEmailToken(String token) {
		EmailVerificationToken evToken = emailVerificationTokenRepository.findByTokenAndUsedFalse(token)
				.orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

		if (evToken.getExpiresAt().isBefore(Instant.now())) {
			throw new IllegalArgumentException("Expired token");
		}

		evToken.setUsed(true);
		emailVerificationTokenRepository.save(evToken);

		User user = evToken.getUser();
		// Mark email verified if you track it; here we can update user's status or auth method verified flag
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
	}
}
