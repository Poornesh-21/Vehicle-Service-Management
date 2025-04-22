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
public class CustomerService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all customers from API
     */
    public List<Map<String, Object>> getAllCustomers(String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("Error fetching customers: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get customer by ID
     */
    public Map<String, Object> getCustomerById(Integer customerId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error fetching customer details: {}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }

    /**
     * Create a new customer
     */
    public Map<String, Object> createCustomer(Map<String, Object> customerData, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage(), e);
            throw new RuntimeException("Error creating customer: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing customer
     */
    public Map<String, Object> updateCustomer(Integer customerId, Map<String, Object> customerData, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
            } else {
                log.warn("Unexpected response status: {}", response.getStatusCode());
                return Collections.emptyMap();
            }
        } catch (Exception e) {
            log.error("Error updating customer: {}", e.getMessage(), e);
            throw new RuntimeException("Error updating customer: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a customer
     */
    public boolean deleteCustomer(Integer customerId, String token) {
        try {
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error deleting customer: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Helper to create auth headers with token
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
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