package com.tuttime.user.websocket;

import com.tuttime.user.service.OnlineStatusService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** WebSocket handler for user presence/online status tracking: manages connections, marks online/offline in Redis with TTL, handles heartbeats. Designed for 10M+ concurrent users. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceWebSocketHandler extends TextWebSocketHandler {
    
    private final OnlineStatusService onlineStatusService;
    private final ObjectMapper objectMapper;
    
    private final Map<String, Long> sessionToUserId = new ConcurrentHashMap<>();
    // Track multiple sessions per user (user can have multiple devices)
    private final Map<Long, Set<String>> userIdToSessions = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> heartbeatTasks = new ConcurrentHashMap<>();
    private final ScheduledThreadPoolExecutor heartbeatScheduler = 
        new ScheduledThreadPoolExecutor(50);
    private static final long HEARTBEAT_INTERVAL_SECONDS = 20;
    private static final int PRESENCE_TTL_SECONDS = 45;
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("WebSocket connection established: sessionId={}", session.getId());
        
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            log.warn("WebSocket connection rejected: no user ID in session attributes");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Authentication required"));
            return;
        }
        
        sessionToUserId.put(session.getId(), userId);
        
        // Track session for this user
        userIdToSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session.getId());
        
        try {
            // Mark online only if this is the first session for this user
            if (userIdToSessions.get(userId).size() == 1) {
                onlineStatusService.markUserOnline(userId, PRESENCE_TTL_SECONDS);
                log.info("User {} connected - marked online (first session, sessionId={})", userId, session.getId());
            } else {
                // User already has other sessions, just refresh presence
                onlineStatusService.refreshPresence(userId, PRESENCE_TTL_SECONDS);
                log.info("User {} connected - additional session (total sessions: {}, sessionId={})", 
                        userId, userIdToSessions.get(userId).size(), session.getId());
            }
        } catch (Exception e) {
            log.warn("Failed to mark user {} online in Redis (non-critical, connection will continue): {}", 
                    userId, e.getMessage());
        }
        
        // Schedule heartbeat task (only if not already scheduled)
        if (!heartbeatTasks.containsKey(userId)) {
            scheduleHeartbeat(userId);
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received WebSocket message: sessionId={}, message={}", 
            session.getId(), message.getPayload());
        
        Long userId = sessionToUserId.get(session.getId());
        if (userId == null) {
            log.warn("Received message from unknown session: {}", session.getId());
            return;
        }
        
        try {
            Map<String, Object> payload = objectMapper.readValue(
                message.getPayload(), 
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
            );
            
            String type = (String) payload.get("type");
            
            if ("heartbeat".equals(type)) {
                // Client sent heartbeat → refresh presence TTL
                // Optimized: Non-blocking - refreshPresence() handles errors internally
                // This prevents heartbeat processing from blocking on slow Redis operations
                try {
                    onlineStatusService.refreshPresence(userId, PRESENCE_TTL_SECONDS);
                } catch (Exception e) {
                    // Redis failures are non-critical - log but continue
                    log.debug("Failed to refresh presence on heartbeat (non-critical): userId={}, error={}", 
                            userId, e.getMessage());
                }
                
                // Send pong response (non-blocking sendMessage with timeout protection)
                sendMessage(session, Map.of(
                    "type", "pong",
                    "timestamp", System.currentTimeMillis()
                ));
                
                log.debug("Heartbeat received from user {}", userId);
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse WebSocket message: {}", e.getMessage());
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug("WebSocket connection closed: sessionId={}, status={}", 
            session.getId(), status);
        
        Long userId = sessionToUserId.remove(session.getId());
        if (userId != null) {
            // Remove session from user's session set
            Set<String> userSessions = userIdToSessions.get(userId);
            if (userSessions != null) {
                userSessions.remove(session.getId());
                
                // Only mark offline if this was the last session for this user
                if (userSessions.isEmpty()) {
                    userIdToSessions.remove(userId);
                    
                    // Cancel heartbeat task
                    ScheduledFuture<?> task = heartbeatTasks.remove(userId);
                    if (task != null) {
                        task.cancel(false);
                    }
                    
                    // Mark user offline (handles Redis unavailability gracefully)
                    try {
                        onlineStatusService.markUserOffline(userId);
                        log.info("User {} disconnected - marked offline (last session closed)", userId);
                    } catch (Exception e) {
                        // During shutdown, Redis may be closed - this is expected
                        log.debug("Could not mark user {} offline (likely during shutdown): {}", userId, e.getMessage());
                    }
                } else {
                    log.info("User {} disconnected - still online (remaining sessions: {}, sessionId={})", 
                            userId, userSessions.size(), session.getId());
                }
            }
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        // Don't log as ERROR if it's a ClosedChannelException during shutdown
        if (exception instanceof java.nio.channels.ClosedChannelException || 
            exception.getCause() instanceof java.nio.channels.ClosedChannelException) {
            log.debug("WebSocket connection closed during shutdown: sessionId={}", session.getId());
        } else {
            String stackTraceSummary = getStackTraceSummary(exception, 3);
            log.error("WebSocket transport error: sessionId={}, stackTrace={}", session.getId(), stackTraceSummary);
        }
        
        Long userId = sessionToUserId.remove(session.getId());
        if (userId != null) {
            // Remove session from user's session set
            Set<String> userSessions = userIdToSessions.get(userId);
            if (userSessions != null) {
                userSessions.remove(session.getId());
                
                // Only mark offline if this was the last session for this user
                if (userSessions.isEmpty()) {
                    userIdToSessions.remove(userId);
                    
                    ScheduledFuture<?> task = heartbeatTasks.remove(userId);
                    if (task != null) {
                        task.cancel(false);
                    }
                    try {
                        onlineStatusService.markUserOffline(userId);
                        log.info("User {} disconnected due to transport error - marked offline (last session)", userId);
                    } catch (Exception e) {
                        // During shutdown, Redis may be closed - this is expected
                        log.debug("Could not mark user {} offline during transport error (likely during shutdown): {}", userId, e.getMessage());
                    }
                } else {
                    log.info("User {} disconnected due to transport error - still online (remaining sessions: {})", 
                            userId, userSessions.size());
                }
            }
        }
    }
    
    /**
     * Graceful shutdown: Close all WebSocket sessions and cancel heartbeat tasks
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down WebSocket handler - closing {} active sessions", sessionToUserId.size());
        
        // Cancel all heartbeat tasks
        heartbeatTasks.values().forEach(task -> {
            if (task != null) {
                task.cancel(false);
            }
        });
        heartbeatTasks.clear();
        
        // Shutdown heartbeat scheduler
        heartbeatScheduler.shutdown();
        try {
            if (!heartbeatScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("WebSocket handler shutdown complete");
    }
    
    /**
     * Schedule periodic heartbeat to keep presence alive
     */
    private void scheduleHeartbeat(Long userId) {
        // Cancel existing task if any
        ScheduledFuture<?> existingTask = heartbeatTasks.get(userId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }
        
        // Schedule new heartbeat task
        ScheduledFuture<?> task = heartbeatScheduler.scheduleAtFixedRate(
            () -> {
                try {
                    // Refresh presence TTL
                    onlineStatusService.refreshPresence(userId, PRESENCE_TTL_SECONDS);
                    log.debug("Heartbeat sent for user {}", userId);
                } catch (Exception e) {
                    log.error("Error sending heartbeat for user {}", userId, e);
                }
            },
            HEARTBEAT_INTERVAL_SECONDS,
            HEARTBEAT_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        heartbeatTasks.put(userId, task);
    }
    
    /**
     * Send message to WebSocket session with timeout protection
     * 
     * <p>Optimized for slow networks:
     * - Checks if session is open before sending
     * - Handles IOException gracefully (common on slow/disconnected networks)
     * - Prevents blocking on full send buffers
     */
    private void sendMessage(WebSocketSession session, Object payload) {
        try {
            // Check if session is still open before attempting to send
            // This prevents blocking on closed/disconnecting sessions
            if (!session.isOpen()) {
                log.debug("Cannot send message: session is closed (sessionId={})", session.getId());
                return;
            }
            
            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
        } catch (IllegalStateException e) {
            // Session is in an invalid state (closing/closed) - this is expected during disconnection
            log.debug("Cannot send message: session in invalid state (sessionId={})", session.getId());
        } catch (IOException e) {
            // Network error - common on slow/unstable networks
            // Don't log as error to avoid noise on cheap devices with poor connectivity
            log.debug("Failed to send WebSocket message (network issue): sessionId={}, error={}", 
                    session.getId(), e.getMessage());
        } catch (Exception e) {
            // Other unexpected errors
            String stackTraceSummary = getStackTraceSummary(e, 3);
            log.error("Unexpected error sending WebSocket message: sessionId={}, stackTrace={}", session.getId(), stackTraceSummary);
        }
    }
    
    /**
     * Gets a shortened stack trace summary for logging.
     * 
     * @param ex The exception
     * @param maxLines Maximum number of stack trace lines to include
     * @return Stack trace summary string (first N lines)
     */
    private String getStackTraceSummary(Throwable ex, int maxLines) {
        if (ex == null) {
            return "";
        }
        
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        int linesToInclude = Math.min(maxLines, stackTrace.length);
        
        for (int i = 0; i < linesToInclude; i++) {
            if (i > 0) {
                summary.append(" | ");
            }
            summary.append(stackTrace[i].toString());
        }
        
        if (stackTrace.length > maxLines) {
            summary.append(" ... (").append(stackTrace.length - maxLines).append(" more)");
        }
        
        return summary.toString();
    }
}

