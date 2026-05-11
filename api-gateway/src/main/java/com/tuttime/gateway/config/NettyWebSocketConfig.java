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

@Configuration
public class NettyWebSocketConfig {

	@Bean
	public NettyReactiveWebServerFactory nettyReactiveWebServerFactory() {
		NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
		factory.addServerCustomizers(httpServer -> httpServer
				.idleTimeout(Duration.ofSeconds(3600)) // server‑side idle timeout
		);
		return factory;
	}
}
