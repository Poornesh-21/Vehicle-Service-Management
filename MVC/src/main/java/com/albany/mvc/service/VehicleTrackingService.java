package com.albany.mvc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all vehicles under service
     */
    public List<Map<String, Object>> getVehiclesUnderService(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/vehicles-under-service",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching vehicles under service: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get all completed services
     */
    public List<Map<String, Object>> getCompletedServices(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/completed-services",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching completed services: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get service request details by ID
     */
    public Map<String, Object> getServiceRequestById(Integer id, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + id,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error fetching service request: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Update service request status
     */
    public boolean updateServiceStatus(Integer requestId, String status, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Collections.singletonMap("status", status);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error updating service status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Record payment for a service
     */
    public boolean recordPayment(Integer requestId, Map<String, Object> paymentDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentDetails, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/payment",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error recording payment: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generate invoice for a service
     */
    public boolean generateInvoice(Integer requestId, Map<String, Object> invoiceDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invoiceDetails, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/invoice",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error generating invoice: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Dispatch a vehicle
     */
    public boolean dispatchVehicle(Integer requestId, Map<String, Object> dispatchDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(dispatchDetails, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/dispatch",
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error dispatching vehicle: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Filter vehicles under service
     */
    public List<Map<String, Object>> filterVehiclesUnderService(Map<String, Object> filterCriteria, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(filterCriteria, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/vehicles-under-service/filter",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error filtering vehicles: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Filter completed services
     */
    public List<Map<String, Object>> filterCompletedServices(Map<String, Object> filterCriteria, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(filterCriteria, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/completed-services/filter",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error filtering services: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Helper method to create authentication headers
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}