package com.tuttime.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import javax.crypto.spec.SecretKeySpec;
import java.net.URI;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
@EnableWebFluxSecurity // 1. Notice this is for WebFlux (Reactive)
public class GatewaySecurityConfig {

	// 1. Inject the exact same secret used by your user-service
	@Value("${spring.security.jwt.secret}")
	private String jwtSecret;

	@Bean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		return http
				// 2. Disable CSRF for stateless APIs
				.csrf(ServerHttpSecurity.CsrfSpec::disable)

				.authorizeExchange(exchanges -> exchanges
						// 3. EXPLICITLY allow anyone to access Swagger and OpenAPI paths
						.pathMatchers(HttpMethod.GET, "/restaurant-medias/**").permitAll()
						.pathMatchers(HttpMethod.GET, "/menu-medias/**").permitAll()

						.pathMatchers(
								"/api/v1/**",

								"/swagger-ui.html",
								"/swagger-ui/**",
								"/v3/api-docs/**",
								"/webjars/**",
								"/user-docs",
								"/user-docs/**",
								"/restaurant-docs",
								"/restaurant-docs/**",
								"/menu-docs",
								"/menu-docs/**",
								"/booking-docs",
								"/booking-docs/**",
								"/order-docs",
								"/order-docs/**",
								"/payment-docs",
								"/payment-docs/**",
								"/pos-docs",
								"/pos-docs/**",
								"/review-docs",
								"/review-docs/**",
								"/analytics-docs",
								"/analytics-docs/**"
						).permitAll()

						// 4. Role-based matching (optional, coarse-grained gating)
//						.pathMatchers("/api/v1/users/**").hasRole("ADMIN")

						// 5. All other gateway traffic requires a valid login token
						.anyExchange().authenticated()
				)

				// 6. Tell the Gateway to act as an OAuth2 Resource Server
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.build();
	}

	@Bean
	public ReactiveJwtDecoder reactiveJwtDecoder() {
		// Fallback safety net to prevent startup failure
		String secureSecret = jwtSecret;
		if (secureSecret.equals("ThisIsAFallbackSecretKeyThatIsAtLeast32BytesLong")) {
			System.out.println("⚠️ WARNING: API Gateway is using the fallback JWT secret. Ensure you define 'spring.security.jwt.access-token.secret' in your application.yml!");
		}

		SecretKeySpec secretKey = new SecretKeySpec(
				secureSecret.getBytes(),
				"HmacSHA256"
		);

		return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
	}

	@Bean
	public RouterFunction<ServerResponse> swaggerRedirects() {
		return route(GET("/swagger-ui/index.html"), req ->
				ServerResponse.temporaryRedirect(URI.create("/swagger-ui.html")).build()
		);
	}
}
