package com.tuttime.notification.provider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InAppProvider {
	private final SimpMessagingTemplate messagingTemplate;

	public void sendRealTimeUpdate(String userId, String title, String content) {
		// Sends to a private queue for the specific user
		String destination = "/queue/notifications/" + userId;
		Map<String, String> payload = Map.of("title", title, "message", content);
		messagingTemplate.convertAndSend(destination, payload);

		log.info("InApp successfully sent to: {}, title: {}, content: {}", userId, title, content);
	}
}
