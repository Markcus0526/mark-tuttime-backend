package com.tuttime.admin.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient Configuration for REST API calls
 * <p>
 * CRITICAL: Configured with timeouts to prevent hanging requests to non-existent services
 * CRITICAL: Configured with increased buffer size to handle large responses (e.g., user lists)
 */
@Configuration
public class WebClientConfig {

	@Bean
	public WebClient.Builder webClientBuilder() {
		// Configure HttpClient with timeouts to prevent hanging on connection attempts
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 5 seconds connection timeout
				.responseTimeout(Duration.ofSeconds(5)) // 5 seconds response timeout
				.doOnConnected(conn ->
						conn.addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
								.addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS))
				);

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				// Increase buffer size to handle large responses (default is 256KB)
				// This fixes DataBufferLimitException when receiving large user lists
				.codecs(configurer -> {
					configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024); // 10MB buffer
				});
	}
}

