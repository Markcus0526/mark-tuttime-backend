package com.tuttime.analytics.config;

import net.devh.boot.grpc.server.security.authentication.BearerAuthenticationReader;
import net.devh.boot.grpc.server.security.authentication.GrpcAuthenticationReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcSecurityConfig {

	@Bean
	public GrpcAuthenticationReader grpcAuthenticationReader() {
		// This tells gRPC to look for the "Authorization: Bearer <token>" header
		return new BearerAuthenticationReader(token ->
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(token, token)
		);
	}
}