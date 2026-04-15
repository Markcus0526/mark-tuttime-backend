package com.tuttime.booking.service.impl;

import com.tuttime.booking.event.WaitlistNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class NotificationDispatcher {

	@Async // Requires @EnableAsync in your JpaConfig or App class
	@EventListener
	@Transactional(readOnly = true)
	public void dispatchNotification(WaitlistNotificationEvent event) {
		log.info("Sending Push Notification to user {}: {}", event.userId(), event.message());

		// TODO: Integrate with FCM (Firebase), Twilio (SMS), or JavaMailSender
		// Example: firebaseService.sendPush(event.userId(), event.message());

		log.info("Notification sent successfully for waitlist {}", event.waitlistId());
	}
}
