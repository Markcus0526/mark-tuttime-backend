package com.tuttime.notification.provider;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PushProvider {
	public void sendPush(String token, String title, String body) {
		try {
			Message message = Message.builder()
					.setToken(token)
					.setNotification(Notification.builder()
							.setTitle(title)
							.setBody(body)
							.build())
					.build();
			FirebaseMessaging.getInstance().send(message);

			log.info("Push successfully sent token: {}, title: {}, body: {}", token, title, body);
		} catch (FirebaseMessagingException e) {
			log.error("FCM Delivery failed: {}", e.getMessage());
			throw new RuntimeException(e);
		}
	}
}