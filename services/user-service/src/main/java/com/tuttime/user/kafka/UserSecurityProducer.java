package com.tuttime.user.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserSecurityProducer {

	private final EventProducer eventProducer;
	private static final String TOPIC = "user-security-events";

	/**
	 * Broadcasts that a user has been blocked in the PostgreSQL DB.
	 */
	public void sendUserBlocked(String userId) {
		UserEvent.SecurityEvent event = new UserEvent.SecurityEvent(userId, "USER_BLOCKED", null, 0);
		eventProducer.sendUserSecurityEvent(event);
		log.info("Broadcasted USER_BLOCKED event for user: {}", userId);
	}

	/**
	 * Broadcasts that a user has been unblocked.
	 */
	public void sendUserUnblocked(String userId) {
		UserEvent.SecurityEvent event = new UserEvent.SecurityEvent(userId, "USER_UNBLOCKED", null, 0);
		eventProducer.sendUserSecurityEvent(event);
		log.info("Broadcasted USER_UNBLOCKED event for user: {}", userId);
	}

	/**
	 * Broadcasts a JTI revocation (Logout/Password change).
	 * @param userId The affected user
	 * @param jti The unique JWT ID to blacklist
	 * @param ttlMillis How much longer the token was valid for
	 */
	public void sendTokenRevoked(String userId, String jti, long ttlMillis) {
		UserEvent.SecurityEvent event = new UserEvent.SecurityEvent(userId, "TOKEN_REVOKED", jti, ttlMillis);
		eventProducer.sendUserSecurityEvent(event);
		log.info("Broadcasted TOKEN_REVOKED for JTI: {}", jti);
	}

	public void sendUserDeleted(String userId) {
		// We reuse the "USER_BLOCKED" type logic at the Gateway
		// because a deleted user should never be allowed to authenticate again.
		UserEvent.SecurityEvent event = new UserEvent.SecurityEvent(userId, "USER_DELETED", null, 0);
		eventProducer.sendUserSecurityEvent(event);
		log.info("Broadcasted USER_DELETED event for user: {}", userId);
	}
}