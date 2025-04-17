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

        // Create a map with only the fields expected by the REST API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("firstName", advisorDto.getFirstName());
        requestBody.put("lastName", advisorDto.getLastName());
        requestBody.put("email", advisorDto.getEmail());
        requestBody.put("phoneNumber", advisorDto.getPhoneNumber());
        requestBody.put("password", advisorDto.getPassword());
        requestBody.put("department", advisorDto.getDepartment());
        requestBody.put("specialization", advisorDto.getSpecialization());

        // Log the request for debugging
        log.debug("Creating service advisor with request: {}", requestBody);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

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
        }

        return null;
    }

    public ServiceAdvisorDto updateServiceAdvisor(Integer id, ServiceAdvisorDto advisorDto, String token) {
        String url = apiBaseUrl + "/service-advisors/" + id;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create a map with only the fields expected by the REST API
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("firstName", advisorDto.getFirstName());
        requestBody.put("lastName", advisorDto.getLastName());
        requestBody.put("email", advisorDto.getEmail());
        requestBody.put("phoneNumber", advisorDto.getPhoneNumber());
        requestBody.put("department", advisorDto.getDepartment());
        requestBody.put("specialization", advisorDto.getSpecialization());

        // Add password only if provided
        if (advisorDto.getPassword() != null && !advisorDto.getPassword().isEmpty()) {
            requestBody.put("password", advisorDto.getPassword());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

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