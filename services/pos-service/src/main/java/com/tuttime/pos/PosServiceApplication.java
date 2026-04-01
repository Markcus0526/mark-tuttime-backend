package com.tuttime.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.tuttime")
@EnableDiscoveryClient
@EnableScheduling
@EnableRetry
public class PosServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(PosServiceApplication.class, args);
	}
}
