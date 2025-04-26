package com.albany.mvc.service;

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

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            log.info("Status update response: {}", response.getBody());
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error updating service status: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Record payment for a service
     */
    public Map<String, Object> recordPayment(Integer requestId, Map<String, Object> paymentDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(paymentDetails, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/payment",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error recording payment: {}", e.getMessage(), e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Generate bill for a service
     * @param requestId the service request ID
     * @param billData the bill data containing materials, labor charges, etc.
     * @param token the authentication token
     * @return the response from the API
     */
    public Map<String, Object> generateBill(Integer requestId, Map<String, Object> billData, String token) {
        try {
            log.info("Generating bill for service request {}", requestId);

            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(billData, headers);

            // Make API call to the REST service
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/bills/service-request/" + requestId + "/generate",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Bill generation API response status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    Map<String, Object> responseMap = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    log.debug("Bill generated successfully for service request {}", requestId);
                    return responseMap;
                } catch (Exception e) {
                    log.error("Error parsing bill generation response: {}", e.getMessage(), e);
                    return Collections.singletonMap("error", "Error parsing response: " + e.getMessage());
                }
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.singletonMap("error", "Unexpected response status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage(), e);
            return Collections.singletonMap("error", "Failed to generate bill: " + e.getMessage());
        }
    }

    /**
     * Generate invoice for a service
     */
    public Map<String, Object> generateInvoice(Integer requestId, Map<String, Object> invoiceDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invoiceDetails, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/invoice",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error generating invoice: {}", e.getMessage(), e);
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * Dispatch a vehicle
     */
    public Map<String, Object> dispatchVehicle(Integer requestId, Map<String, Object> dispatchDetails, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(dispatchDetails, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicle-tracking/service-request/" + requestId + "/dispatch",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error dispatching vehicle: {}", e.getMessage(), e);
            return Collections.singletonMap("error", e.getMessage());
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
     * Search for vehicles and services
     */
    public Map<String, List<Map<String, Object>>> searchVehiclesAndServices(String query, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);

            Map<String, Object> searchCriteria = Collections.singletonMap("search", query);

            // Get vehicles under service matching the search
            List<Map<String, Object>> vehiclesUnderService = filterVehiclesUnderService(searchCriteria, token);

            // Get completed services matching the search
            List<Map<String, Object>> completedServices = filterCompletedServices(searchCriteria, token);

            // Combine results
            Map<String, List<Map<String, Object>>> results = new HashMap<>();
            results.put("vehiclesUnderService", vehiclesUnderService);
            results.put("completedServices", completedServices);

            return results;
        } catch (Exception e) {
            log.error("Error searching vehicles and services: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Helper method to create authentication headers
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