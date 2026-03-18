package com.tuttime.admin.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewayConfig Tests")
class GatewayConfigTest {

    private final GatewayConfig gatewayConfig = new GatewayConfig();

    @Test
    @DisplayName("Should create RestTemplate bean")
    void testRestTemplate_ShouldCreateBean() {
        // When
        RestTemplate restTemplate = gatewayConfig.restTemplate();

        // Then
        assertThat(restTemplate).isNotNull();
    }

    @Test
    @DisplayName("LoadBalanced RestTemplate should be load balanced")
    void testRestTemplate_ShouldBeLoadBalanced() throws NoSuchMethodException {
        // Given
        Method loadBalancedRestTemplateMethod = GatewayConfig.class.getMethod("loadBalancedRestTemplate");

        // Then
        assertThat(loadBalancedRestTemplateMethod.isAnnotationPresent(LoadBalanced.class))
                .as("LoadBalanced RestTemplate should be annotated with @LoadBalanced")
                .isTrue();
    }
    
    @Test
    @DisplayName("Primary RestTemplate should not be load balanced")
    void testRestTemplate_ShouldNotBeLoadBalanced() throws NoSuchMethodException {
        // Given
        Method restTemplateMethod = GatewayConfig.class.getMethod("restTemplate");

        // Then
        assertThat(restTemplateMethod.isAnnotationPresent(LoadBalanced.class))
                .as("Primary RestTemplate should NOT be annotated with @LoadBalanced")
                .isFalse();
    }
}

