package com.tuttime.user.config;

import com.tuttime.user.websocket.PresenceWebSocketHandler;
import com.tuttime.user.websocket.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** WebSocket configuration for Presence/Online Status tracking. */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class PresenceWebSocketConfig implements WebSocketConfigurer {
    
    private final PresenceWebSocketHandler presenceWebSocketHandler;
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(presenceWebSocketHandler, "/ws/presence")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOrigins("*");
        
        log.info("Presence WebSocket endpoint registered: /ws/presence");
    }
}

