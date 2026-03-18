package com.tuttime.gateway.config;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;

import java.time.Duration;

@Slf4j
@Configuration
public class NettyWebSocketConfig {

    @Bean
    public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        
        factory.addServerCustomizers(new NettyServerCustomizer() {
            @Override
            public HttpServer apply(HttpServer httpServer) {
                return httpServer
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .idleTimeout(Duration.ofSeconds(3600)); // 1 hour server timeout
            }
        });
        
        log.info("Netty WebSocket configuration applied: idle timeout set to 3600 seconds (1 hour)");
        return factory;
    }

    /**
     * Custom HTTP client configuration for WebSocket connections.
     * This overrides the default HTTP client settings to prevent premature WebSocket connection closures.
     * The default max-keep-alive-time (30s) and response-timeout (60s) are too short for long-lived WebSocket connections.
     */
    @Bean
    public HttpClientCustomizer httpClientCustomizer() {
        return new HttpClientCustomizer() {
            @Override
            public HttpClient customize(HttpClient httpClient) {
                return httpClient
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000) // 60 seconds connection timeout
                        .responseTimeout(Duration.ofSeconds(3600)) // 1 hour server timeout for WebSocket
                        .keepAlive(true)
                        .doOnConnected(conn -> {
                            conn.addHandlerLast(new io.netty.handler.timeout.IdleStateHandler(
                                    3600, // read idle timeout: 1 hour (server timeout)
                                    3600, // write idle timeout: 1 hour (server timeout)
                                    3600  // all idle timeout: 1 hour (server timeout)
                            ));
                        });
            }
        };
    }
}

