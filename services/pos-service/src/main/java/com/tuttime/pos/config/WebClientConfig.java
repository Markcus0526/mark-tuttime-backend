package com.tuttime.pos.config;

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

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient posWebClient(WebClient.Builder builder) {
		// Configure HttpClient with strict timeouts
		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 5s to establish connection
				.responseTimeout(Duration.ofSeconds(10))           // 10s for total response
				.doOnConnected(conn -> conn
						.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
						.addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));

		return builder
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				// Ensure both JSON and XML codecs are present for iiko and R-Keeper
				.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
				.build();
	}
}