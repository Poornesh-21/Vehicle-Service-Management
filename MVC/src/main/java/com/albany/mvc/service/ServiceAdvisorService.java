package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceAdvisorDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    public List<ServiceAdvisorDto> getAllServiceAdvisors(String token) {
        String url = apiBaseUrl + "/service-advisors";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // Debug logging for token
            log.debug("Fetching service advisors with token: {}", token.substring(0, Math.min(10, token.length())) + "...");
            log.debug("Authorization header: {}", headers.getFirst("Authorization"));

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("Response status: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("Response body: {}", response.getBody());
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<ServiceAdvisorDto>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error fetching service advisors: {}", e.getMessage());
            try {
                // For debugging - try to log any response body
                if (e.getMessage().contains("response")) {
                    log.debug("Error response details: {}", e.getMessage());
                }
            } catch (Exception ignored) {}
        }

        return Collections.emptyList();
    }

    public ServiceAdvisorDto getServiceAdvisorById(Integer id, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error fetching service advisor: {}", e.getMessage());
        }

        return null;
    }

    public ServiceAdvisorDto createServiceAdvisor(ServiceAdvisorDto advisorDto, String token) {
        String url = apiBaseUrl + "/service-advisors";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Log the entire request
        log.debug("Creating service advisor with request: {}", advisorDto);
        log.debug("API URL: {}", url);
        log.debug("Auth token first 10 chars: {}", token.substring(0, Math.min(10, token.length())));

        HttpEntity<ServiceAdvisorDto> entity = new HttpEntity<>(advisorDto, headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ServiceAdvisorDto.class
            );

            log.debug("Service advisor creation response: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error creating service advisor: {}", e.getMessage());

            // Try to extract more details from the exception
            if (e.getMessage().contains("403")) {
                log.error("Access denied - check that the token has the correct permissions");
            } else if (e.getMessage().contains("400")) {
                log.error("Bad request - check that the request body is correctly structured");
            }

            // Try to log the response body if available
            try {
                String errorDetails = e.getMessage();
                log.error("Detailed error: {}", errorDetails);
            } catch (Exception ignored) {}
        }

        return null;
    }

    public ServiceAdvisorDto updateServiceAdvisor(Integer id, ServiceAdvisorDto advisorDto, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Log the request
        log.debug("Updating service advisor with ID: {} and data: {}", id, advisorDto);

        HttpEntity<ServiceAdvisorDto> entity = new HttpEntity<>(advisorDto, headers);

        try {
            ResponseEntity<ServiceAdvisorDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    ServiceAdvisorDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            log.error("Error updating service advisor: {}", e.getMessage());
        }

        return null;
    }

    public boolean deleteServiceAdvisor(Integer id, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            return response.getStatusCode() == HttpStatus.NO_CONTENT;

        } catch (Exception e) {
            log.error("Error deleting service advisor: {}", e.getMessage());
        }

        return false;
    }
}