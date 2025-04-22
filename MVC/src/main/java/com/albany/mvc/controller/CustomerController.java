package com.albany.mvc.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for customer operations
 */
@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Create a new customer with improved error handling
     */
    @PostMapping("/customers")
    @ResponseBody
    public ResponseEntity<?> createCustomer(
            @RequestBody Map<String, Object> customerData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Creating customer with data: {}", customerData);

        try {
            // Get token from various sources
            String validToken = getValidToken(token, authHeader, request);

            if (validToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Add a temporary password if not provided
            if (!customerData.containsKey("password") ||
                    customerData.get("password") == null ||
                    customerData.get("password").toString().isEmpty()) {
                customerData.put("password", generateTempPassword());
            }

            // Forward the request to the backend API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

            // Make the API call to the backend
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/customers",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Customer created successfully");

            // Parse and return the response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                try {
                    Map<String, Object> responseBody = objectMapper.readValue(
                            response.getBody(),
                            new TypeReference<Map<String, Object>>() {}
                    );
                    return ResponseEntity.status(response.getStatusCode()).body(responseBody);
                } catch (Exception e) {
                    log.warn("Error parsing success response body: {}", e.getMessage());
                    return ResponseEntity.ok(response.getBody());
                }
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
        } catch (HttpClientErrorException e) {
            log.error("API client error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());

            // Try to parse and return the detailed error message from the API
            try {
                Map<String, Object> errorResponse = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        new TypeReference<Map<String, Object>>() {}
                );

                return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
            } catch (Exception ex) {
                log.error("Error parsing error response: {}", ex.getMessage());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to create customer: " + e.getMessage());
                return ResponseEntity.status(e.getStatusCode()).body(errorResponse);
            }
        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create customer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Generate a temporary password for new customers
     */
    private String generateTempPassword() {
        final String letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // Excluded confusing characters
        final String numbers = "123456789"; // Excluded 0 to avoid confusion with O

        StringBuilder password = new StringBuilder("CUS2025-");

        // Add 3 random letters
        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * letters.length());
            password.append(letters.charAt(index));
        }

        // Add 3 random numbers
        for (int i = 0; i < 3; i++) {
            int index = (int) (Math.random() * numbers.length());
            password.append(numbers.charAt(index));
        }

        return password.toString();
    }

    /**
     * Gets a valid token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Using token from Authorization header");
            return authHeader.substring(7);
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                log.debug("Using token from session");
                return sessionToken;
            }
        }

        log.warn("No valid token found from any source");
        return null;
    }
}