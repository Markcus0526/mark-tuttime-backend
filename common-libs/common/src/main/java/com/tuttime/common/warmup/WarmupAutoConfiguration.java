package com.tuttime.common.warmup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
public class WarmupAutoConfiguration {

	@Component
	@ConditionalOnBean(JpaRepository.class)
	@RequiredArgsConstructor
	static class DatabaseWarmup implements ApplicationListener<ApplicationReadyEvent> {
		private final List<JpaRepository<?, ?>> repositories;

		@Override
		public void onApplicationEvent(ApplicationReadyEvent event) {
			log.info("Starting database warmup for {} repositories", repositories.size());
			for (JpaRepository<?, ?> repo : repositories) {
				try {
					repo.count();
					log.debug("Warmed up repository: {}", repo.getClass().getSimpleName());
				} catch (Exception e) {
					log.warn("Failed to warmup repository {}: {}", repo.getClass().getSimpleName(), e.getMessage());
				}
			}
		}
	}

	@Component
	@ConditionalOnClass(StringRedisTemplate.class)
	@RequiredArgsConstructor
	static class RedisWarmup implements ApplicationListener<ApplicationReadyEvent> {
		private final StringRedisTemplate redisTemplate;

		@Override
		public void onApplicationEvent(ApplicationReadyEvent event) {
			log.info("Warming up Redis");
			try {
				redisTemplate.opsForValue().get("warmup");
				log.debug("Redis warmup successful");
			} catch (Exception e) {
				log.warn("Redis warmup failed: {}", e.getMessage());
			}
		}
	}
}