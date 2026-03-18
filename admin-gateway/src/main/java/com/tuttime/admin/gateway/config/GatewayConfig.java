package com.tuttime.admin.gateway.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** Gateway Configuration: configures gateway components. */
@Configuration
public class GatewayConfig {

    /** RestTemplate with load balancing for service discovery (e.g., admin-service via Eureka) */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /** RestTemplate without load balancing for direct URLs. Primary bean for AdminProxyServiceImpl. Supports multipart/form-data. */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(30000);
        restTemplate.setRequestFactory(factory);
        
        List<HttpMessageConverter<?>> converters = new ArrayList<>(restTemplate.getMessageConverters());
        converters.add(0, new ByteArrayHttpMessageConverter());
        converters.add(new AllEncompassingFormHttpMessageConverter());
        converters.add(new ResourceHttpMessageConverter());
        
        restTemplate.setMessageConverters(converters);
        
        return restTemplate;
    }

    /** MultipartResolver for parsing multipart/form-data requests. */
    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver resolver = new StandardServletMultipartResolver();
        resolver.setResolveLazily(false);
        return resolver;
    }

    /** Register MultipartParsingFilter to ensure multipart requests are parsed. */
    @Bean
    public FilterRegistrationBean<com.tuttime.admin.gateway.filter.MultipartParsingFilter> multipartParsingFilter() {
        FilterRegistrationBean<com.tuttime.admin.gateway.filter.MultipartParsingFilter> registration = 
                new FilterRegistrationBean<>();
        registration.setFilter(new com.tuttime.admin.gateway.filter.MultipartParsingFilter());
        registration.addUrlPatterns("/api/admin/*");
        registration.setOrder(1);
        return registration;
    }


    /** WebClient for handling multipart/form-data requests. */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(500 * 1024 * 1024);
                })
                .build();
    }

    /** ObjectMapper bean for JSON serialization/deserialization. */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

