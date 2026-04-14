package com.tuttime.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtToHeaderFilter implements GlobalFilter, Ordered {

	// Inject the Reactive template we configured earlier
	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getURI().getPath();
		if (path.startsWith("/media/")) {
			return chain.filter(exchange);
		}

		return ReactiveSecurityContextHolder.getContext()
				.flatMap(securityContext -> {
					if (securityContext.getAuthentication() != null &&
							securityContext.getAuthentication().getPrincipal() instanceof Jwt jwt) {

						String jti = jwt.getId(); // Extract unique JWT ID (JTI)
						String userId = jwt.getClaimAsString("userId");

						// 1. Check Redis for JTI Blacklist
						return reactiveRedisTemplate.hasKey("blacklist:jti:" + jti)
								.flatMap(isBlacklisted -> {
									if (Boolean.TRUE.equals(isBlacklisted)) {
										log.warn("Blocking request: JTI {} is blacklisted for user {}", jti, userId);
										exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
										return exchange.getResponse().setComplete();
									}

									// 2. If not blacklisted, proceed with header injection
									List<String> roles = jwt.getClaimAsStringList("roles");
									String rolesString = roles != null ? String.join(",", roles) : "";

									ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
											.header("X-User-Id", userId != null ? userId : "")
											.header("X-User-Roles", rolesString)
											.build();

									return chain.filter(exchange.mutate().request(modifiedRequest).build());
								});
					}
					return chain.filter(exchange);
				})
				.switchIfEmpty(chain.filter(exchange));
	}

	@Override
	public int getOrder() {
		// Run late enough for SecurityContext to be populated, but early enough for routing
		return Ordered.LOWEST_PRECEDENCE - 10;
	}
}
