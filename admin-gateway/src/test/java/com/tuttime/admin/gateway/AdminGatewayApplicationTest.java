package com.tuttime.admin.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AdminGatewayApplication Tests")
class AdminGatewayApplicationTest {

    @Test
    @DisplayName("Application should be annotated with EnableDiscoveryClient")
    void testApplication_ShouldBeAnnotated() {
        // Then
        assertThat(AdminGatewayApplication.class.isAnnotationPresent(EnableDiscoveryClient.class))
                .as("AdminGatewayApplication should be annotated with @EnableDiscoveryClient")
                .isTrue();
    }

    @Test
    @DisplayName("Application main method should exist")
    void testApplication_ShouldHaveMainMethod() throws NoSuchMethodException {
        // Given
        Class<AdminGatewayApplication> clazz = AdminGatewayApplication.class;

        // Then
        assertThat(clazz.getMethod("main", String[].class))
                .as("AdminGatewayApplication should have main method")
                .isNotNull();
    }
}

