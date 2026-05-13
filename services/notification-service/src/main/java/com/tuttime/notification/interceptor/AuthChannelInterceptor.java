package com.tuttime.notification.interceptor;

import com.tuttime.notification.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

	private final JwtTokenUtil jwtTokenUtil;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {
			List<String> authHeaders = accessor.getNativeHeader("Authorization");
			if (authHeaders == null || authHeaders.isEmpty()) {
				log.error("Missing Authorization header, rejecting WebSocket connection");
				throw new SecurityException("Missing token");
			}
			String token = authHeaders.get(0).replace("Bearer ", "");
			try {
				UUID userId = jwtTokenUtil.getUserIdFromToken(token);
				String userIdStr = userId.toString();
				// Store in session attributes (for event listener)
				accessor.getSessionAttributes().put("userId", userIdStr);
				// Set Principal to integrate with Spring Security
				accessor.setUser(() -> userIdStr);
				log.debug("WebSocket authenticated for user {}", userIdStr);
			} catch (Exception e) {
				log.error("Invalid token, rejecting WebSocket connection: {}", e.getClass().getSimpleName());
				throw new SecurityException("Invalid token", e);
			}
		}
		return message;
	}
}