package com.tuttime.notification.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppProvider {
	private final SimpMessagingTemplate messagingTemplate;

	public void sendRealTimeUpdate(String userId, String title, String body) {
		// Send to a queue that only this user subscribes to
		messagingTemplate.convertAndSendToUser(userId, "/queue/notifications",
				Map.of("title", title, "body", body, "timestamp", Instant.now()));

		log.info("InApp successfully sent to: {}, title: {}, content: {}", userId, title, body);
	}
}
