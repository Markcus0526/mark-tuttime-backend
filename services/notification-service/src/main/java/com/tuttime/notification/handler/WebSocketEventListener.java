package com.tuttime.notification.handler;

import com.tuttime.notification.provider.UserSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

	private final UserSessionRegistry sessionRegistry;

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		Principal principal = accessor.getUser();
		if (principal != null) {
			String userId = principal.getName();
			String sessionId = accessor.getSessionId();
			sessionRegistry.add(userId, sessionId);
			log.info("User {} connected, sessionId={}", userId, sessionId);
		} else {
			log.warn("WebSocket connection without authenticated user");
		}
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		String sessionId = accessor.getSessionId();
		Map<String, Object> attrs = accessor.getSessionAttributes();
		String userId = (String) attrs.get("userId");

		if (userId != null) {
			sessionRegistry.remove(userId, sessionId);
			log.info("User {} disconnected, sessionId={}", userId, sessionId);
		} else {
			log.warn("Disconnect without userId, sessionId={}", sessionId);
		}
	}
}