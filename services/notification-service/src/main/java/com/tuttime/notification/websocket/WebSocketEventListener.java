package com.tuttime.notification.websocket;

import com.tuttime.notification.provider.UserSessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

	private final UserSessionRegistry sessionRegistry;

	@EventListener
	public void handleWebSocketConnectListener(SessionConnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

		// Assuming you pass userId in a header or via Spring Security Principal
		String userId = headerAccessor.getFirstNativeHeader("userId");
		String sessionId = headerAccessor.getSessionId();

		if (userId != null) {
			sessionRegistry.add(userId, sessionId);
		}
	}

	@EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

		String userId = headerAccessor.getFirstNativeHeader("userId");
		String sessionId = headerAccessor.getSessionId();

		if (userId != null) {
			sessionRegistry.remove(userId, sessionId);
		}
	}
}
