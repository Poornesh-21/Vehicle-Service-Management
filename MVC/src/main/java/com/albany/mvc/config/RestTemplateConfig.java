package com.albany.mvc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;

/**
 * Configuration for RestTemplate with custom timeouts and error handling
 */
@Configuration
public class RestTemplateConfig {

    @Value("${rest.connection.timeout:5000}")
    private int connectionTimeout;

    @Value("${rest.read.timeout:10000}")
    private int readTimeout;

    /**
     * Creates a RestTemplate with custom timeouts and error handling
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.connectTimeout(Duration.ofMillis(connectionTimeout)).readTimeout(Duration.ofMillis(readTimeout))
                .errorHandler(new CustomResponseErrorHandler())
                .build();
    }
    
    /**
     * Custom error handler that preserves the original status code
     * so we can handle it appropriately in the controller
     */
    private static class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
        @Override
        public boolean hasError(ClientHttpResponse response) throws IOException {
            HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
            return (statusCode != null && hasError(statusCode));
        }
    }
}