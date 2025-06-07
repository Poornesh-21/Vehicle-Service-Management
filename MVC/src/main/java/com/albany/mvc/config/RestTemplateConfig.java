package com.albany.mvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate with properly configured timeouts
 */
@Configuration
public class RestTemplateConfig {

    @Value("${rest.connection.timeout:30000}")
    private int connectionTimeout;

    @Value("${rest.read.timeout:30000}")
    private int readTimeout;

    /**
     * Creates a RestTemplate with proper timeout configuration
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectionTimeout);
        factory.setReadTimeout(readTimeout);

        return new RestTemplate(factory);
    }
}