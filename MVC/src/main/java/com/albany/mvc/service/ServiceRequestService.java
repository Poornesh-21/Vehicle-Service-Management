package com.albany.mvc.service;

import com.albany.mvc.dto.ServiceRequestDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all service requests with improved data validation
     */
    public List<ServiceRequestDto> getAllServiceRequests(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Fetching service requests from API: {}", apiBaseUrl + "/service-requests");
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Raw API response: {}", response.getBody());

                // Parse the response
                List<ServiceRequestDto> requests = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<ServiceRequestDto>>() {}
                );

                // Log and verify each request's data
                if (requests != null) {
                    for (ServiceRequestDto req : requests) {
                        // Apply default values if needed
                        validateAndEnhanceRequest(req);

                        // Log for debugging
                        log.debug("Processed request ID: {}, Status: {}, Membership: {}",
                                req.getRequestId(), req.getStatus(), req.getMembershipStatus());
                    }
                }

                return requests;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching service requests: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch service requests: " + e.getMessage(), e);
        }
    }

    /**
     * Validate and enhance a request with default values if needed
     */
    private void validateAndEnhanceRequest(ServiceRequestDto req) {
        // Ensure request ID is present
        if (req.getRequestId() == null) {
            log.warn("Request has null ID");
        }

        // Ensure status has a valid value
        if (req.getStatus() == null || req.getStatus().trim().isEmpty()) {
            log.warn("Request {} has null/empty status, setting to 'Unknown'", req.getRequestId());
            req.setStatus("Unknown");
        }

        // Ensure membership status has a valid value and preserve exact value from API
        if (req.getMembershipStatus() == null) {
            log.warn("Request {} has null membership status, setting to 'Standard'",
                    req.getRequestId());
            req.setMembershipStatus("Standard");
        } else {
            // Log the membership status we received from the API
            log.debug("Request {} has membership status '{}' from API",
                    req.getRequestId(), req.getMembershipStatus());

            // Do not modify or normalize the membership status value
            // This ensures we preserve exactly what the API sent us
        }

        // Ensure vehicle name is set
        if ((req.getVehicleName() == null || req.getVehicleName().trim().isEmpty()) &&
                req.getVehicleBrand() != null && req.getVehicleModel() != null) {
            req.setVehicleName(req.getVehicleBrand() + " " + req.getVehicleModel());
        }

        // Log complete request data for debugging
        log.debug("Enhanced request: ID={}, Status={}, Membership={}, Vehicle={}",
                req.getRequestId(), req.getStatus(), req.getMembershipStatus(), req.getVehicleName());
    }

    /**
     * Get service request by ID with improved error handling
     */
    public ServiceRequestDto getServiceRequestById(Integer requestId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.info("Fetching service request with ID: {}", requestId);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto request = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                // Validate and enhance the request
                validateAndEnhanceRequest(request);

                return request;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to fetch service request with ID: " + requestId);
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Service request not found: {}", requestId);
            throw new RuntimeException("Service request not found with ID: " + requestId);
        } catch (Exception e) {
            log.error("Error fetching service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch service request: " + e.getMessage());
        }
    }

    /**
     * Create a new service request with improved validation
     */
    public ServiceRequestDto createServiceRequest(ServiceRequestDto requestDto, String token) {
        log.debug("Creating service request: {}", requestDto);

        try {
            // Validate and enhance the request before sending
            validateAndEnhanceRequest(requestDto);

            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ServiceRequestDto> entity = new HttpEntity<>(requestDto, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto createdRequest = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                // Validate and enhance the newly created request
                validateAndEnhanceRequest(createdRequest);

                return createdRequest;
            } else {
                log.error("Unexpected response: {}", response.getStatusCode());
                throw new RuntimeException("Unexpected response from server: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            // Try to parse error response for better error message
            try {
                Map<String, Object> errorResponse = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        new TypeReference<Map<String, Object>>() {}
                );

                String errorMessage = errorResponse.containsKey("message")
                        ? errorResponse.get("message").toString()
                        : e.getMessage();

                throw new RuntimeException("API error: " + errorMessage);
            } catch (Exception ex) {
                throw new RuntimeException("API error: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error creating service request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create service request: " + e.getMessage());
        }
    }

    /**
     * Assign a service advisor with improved validation
     */
    public ServiceRequestDto assignServiceAdvisor(Integer requestId, Integer advisorId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Integer> requestBody = Collections.singletonMap("advisorId", advisorId);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Assigning advisor {} to request {}", advisorId, requestId);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId + "/assign",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto updatedRequest = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                // Validate and enhance the updated request
                validateAndEnhanceRequest(updatedRequest);

                return updatedRequest;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to assign service advisor");
            }
        } catch (Exception e) {
            log.error("Error assigning service advisor: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to assign service advisor: " + e.getMessage());
        }
    }

    /**
     * Update service request status with improved validation
     */
    public ServiceRequestDto updateServiceRequestStatus(Integer requestId, String status, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Collections.singletonMap("status", status);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Updating status of request {} to {}", requestId, status);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-requests/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ServiceRequestDto updatedRequest = objectMapper.readValue(
                        response.getBody(),
                        ServiceRequestDto.class
                );

                // Validate and enhance the updated request
                validateAndEnhanceRequest(updatedRequest);

                return updatedRequest;
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to update service request status");
            }
        } catch (Exception e) {
            log.error("Error updating service request status: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update service request status: " + e.getMessage());
        }
    }

    /**
     * Create authentication headers with token
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        if (token != null && !token.isEmpty()) {
            if (token.startsWith("Bearer ")) {
                headers.set("Authorization", token);
            } else {
                headers.setBearerAuth(token);
            }
        }

        return headers;
    }
}