package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
public class ServiceRequestService {

    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all service requests
     */
    public List<ServiceRequestDto> getAllServiceRequests(String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("Fetching all service requests from API: {}", apiBaseUrl + "/service-requests");

            ResponseEntity<List<ServiceRequestDto>> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ServiceRequestDto>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully fetched {} service requests", response.getBody().size());
                return response.getBody();
            } else {
                log.warn("API returned unsuccessful status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching service requests: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get service request by ID
     */
    public ServiceRequestDto getServiceRequestById(Integer id, String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("Fetching service request details for ID: {}", id);

            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + id,
                    HttpMethod.GET,
                    entity,
                    ServiceRequestDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Successfully fetched service request details");
                return response.getBody();
            } else {
                log.warn("API returned unsuccessful status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching service request details: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create a new service request
     */
    public ServiceRequestDto createServiceRequest(ServiceRequestDto requestDto, String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Make sure the status field is initialized to avoid null pointer issues
            if (requestDto.getStatus() == null) {
                requestDto.setStatus("Received");
            }

            HttpEntity<ServiceRequestDto> entity = new HttpEntity<>(requestDto, headers);

            log.debug("Creating service request: {}", requestDto);

            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests",
                    HttpMethod.POST,
                    entity,
                    ServiceRequestDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Service request created successfully");
                return response.getBody();
            } else {
                log.warn("API returned unsuccessful status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to create service request, API returned: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error creating service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create service request: " + e.getMessage());
        }
    }

    /**
     * Assign service advisor to a service request
     */
    public ServiceRequestDto assignServiceAdvisor(Integer requestId, Integer advisorId, String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Integer> requestBody = new HashMap<>();
            requestBody.put("advisorId", advisorId);

            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Assigning service advisor {} to request {}", advisorId, requestId);

            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId + "/assign",
                    HttpMethod.PUT,
                    entity,
                    ServiceRequestDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Service advisor assigned successfully");
                return response.getBody();
            } else {
                log.warn("API returned unsuccessful status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error assigning service advisor: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Update service request status
     */
    public ServiceRequestDto updateServiceRequestStatus(Integer requestId, String status, String token) {
        try {
            HttpHeaders headers = createHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("status", status);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            log.debug("Updating service request {} status to {}", requestId, status);

            ResponseEntity<ServiceRequestDto> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    ServiceRequestDto.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Service request status updated successfully");
                return response.getBody();
            } else {
                log.warn("API returned unsuccessful status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Error updating service request status: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Helper method to create headers with authentication
     */
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isEmpty()) {
            // Check if token already has Bearer prefix
            if (token.startsWith("Bearer ")) {
                headers.set("Authorization", token);
            } else {
                headers.setBearerAuth(token);
            }
        }
        return headers;
    }
}