package com.tuttime.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.client.ApiClientBuilder;

@Configuration
public class YooKassaConfig {

	@Value("${yookassa.shop-id}")
	private String shopId;

	@Value("${yookassa.secret-key}")
	private String secretKey;

	@Bean
	public ApiClient apiKassaClient() {
		return ApiClientBuilder.newBuilder()
				.configureBasicAuth(shopId, secretKey)
				.build();
	}
}
