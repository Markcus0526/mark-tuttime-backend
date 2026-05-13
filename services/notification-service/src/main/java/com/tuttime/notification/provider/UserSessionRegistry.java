package com.tuttime.notification.provider;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserSessionRegistry {
	private final Map<String, Set<String>> activeUsers = new ConcurrentHashMap<>();

	public void add(String userId, String sessionId) {
		activeUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
	}

	public void remove(String userId, String sessionId) {
		Set<String> sessions = activeUsers.get(userId);
		if (sessions != null) {
			sessions.remove(sessionId);
			if (sessions.isEmpty()) activeUsers.remove(userId);
		}
	}

	public boolean isUserOnline(String userId) {
		return activeUsers.containsKey(userId);
	}
}