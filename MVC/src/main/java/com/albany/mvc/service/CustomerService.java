package com.albany.mvc.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
                List<Map<String, Object>> customers = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                // Process each customer to format dates properly
                customers.forEach(this::processCustomerDates);

                return customers;
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
                Map<String, Object> customer = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Process dates for this customer
                processCustomerDates(customer);

                return customer;
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
     * Process customer dates to ensure they're properly formatted for display
     */
    private void processCustomerDates(Map<String, Object> customer) {
        // Handle lastServiceDate
        if (customer.containsKey("lastServiceDate") && customer.get("lastServiceDate") != null) {
            String formattedDate;
            try {
                // Try to parse as LocalDate
                Object dateObj = customer.get("lastServiceDate");
                LocalDate date;

                if (dateObj instanceof String) {
                    // If it's a string, try to parse it as ISO date (yyyy-MM-dd)
                    date = LocalDate.parse((String) dateObj);
                } else if (dateObj instanceof Long) {
                    // If it's a timestamp (milliseconds since epoch)
                    date = LocalDate.ofEpochDay((Long) dateObj / (24*60*60*1000));
                } else {
                    // Otherwise use toString and try to parse
                    date = LocalDate.parse(dateObj.toString());
                }

                // Format the date in a nice readable format
                formattedDate = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            } catch (DateTimeParseException e) {
                log.warn("Could not parse date: {}", customer.get("lastServiceDate"));
                formattedDate = "Invalid date format";
            }

            // Add the formatted date to the customer data
            customer.put("formattedLastServiceDate", formattedDate);
        } else {
            customer.put("formattedLastServiceDate", "No service yet");
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
                Map<String, Object> customer = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Process dates for the newly created customer
                processCustomerDates(customer);

                return customer;
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
                Map<String, Object> customer = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );

                // Process dates for the updated customer
                processCustomerDates(customer);

                return customer;
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
}