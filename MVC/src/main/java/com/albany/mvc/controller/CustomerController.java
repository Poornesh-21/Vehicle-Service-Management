package com.albany.mvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Render the customers page
     */
    @GetMapping
    public String customersPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        log.info("Accessing customers page");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/admin/login?error=session_expired";
        }

        // Set the admin's name for the page
        model.addAttribute("userName", "Arthur Morgan");

        return "admin/customers";
    }

    /**
     * Get all customers via API
     */
    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        try {
            // Get token from various sources
            String validToken = getValidToken(token, authHeader, request);

            if (validToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
            }

            // Forward the request to the backend API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Make the API call to the backend
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    apiBaseUrl + "/customers",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            log.debug("API response for all customers: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error fetching customers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Get a specific customer by ID
     */
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCustomerById(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        try {
            // Get token from various sources
            String validToken = getValidToken(token, authHeader, request);

            if (validToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Forward the request to the backend API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Make the API call to the backend
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + id,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.debug("Customer API response status: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error fetching customer details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new customer
     */
    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createCustomer(
            @RequestBody Map<String, Object> customerData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        try {
            // Get token from various sources
            String validToken = getValidToken(token, authHeader, request);

            if (validToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Forward the request to the backend API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

            // Make the API call to the backend
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/customers",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.info("Customer created successfully");
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to create customer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Update an existing customer
     */
    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> customerData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        try {
            // Get token from various sources
            String validToken = getValidToken(token, authHeader, request);

            if (validToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Forward the request to the backend API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(customerData, headers);

            // Make the API call to the backend
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + id,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.info("Customer updated successfully");
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error updating customer: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update customer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Delete a customer
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteCustomer(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        try {
            // Get token from various sources
            String validToken = getValidToken(token, authHeader, request);

            if (validToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Forward the request to the backend API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Make the API call to the backend
            ResponseEntity<Void> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + id,
                    HttpMethod.DELETE,
                    entity,
                    Void.class
            );

            log.info("Customer deleted successfully");
            return ResponseEntity.status(response.getStatusCode()).build();

        } catch (Exception e) {
            log.error("Error deleting customer: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete customer: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Gets a valid token from various sources
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }

    /**
     * Gets a valid token from various sources with Auth header
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            // Store token in session
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", tokenParam);
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