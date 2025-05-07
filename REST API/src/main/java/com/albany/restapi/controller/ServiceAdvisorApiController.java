package com.albany.restapi.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/serviceAdvisor/api")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorApiController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get assigned vehicles for the authenticated service advisor
     */
    @GetMapping("/assigned-vehicles")
    public ResponseEntity<List<Map<String, Object>>> getAssignedVehicles(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for assigned vehicles request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/assigned-vehicles",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> vehicles = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return ResponseEntity.ok(vehicles);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Error fetching assigned vehicles: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Get new service assignments for service advisors
     */
    @GetMapping("/new-assignments")
    public ResponseEntity<List<Map<String, Object>>> getNewAssignments(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for new assignments request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/new-assignments",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> assignments = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                return ResponseEntity.ok(assignments);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyList());
            }
        } catch (Exception e) {
            log.error("Error fetching new assignments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    /**
     * Get service request details
     */
    @GetMapping("/service-details/{requestId}")
    public ResponseEntity<Map<String, Object>> getServiceDetails(
            @PathVariable Integer requestId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for service details request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/" + requestId + "/details",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> details = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(details);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyMap());
            }
        } catch (Exception e) {
            log.error("Error fetching service details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    /**
     * Add inventory items to a service request
     */
    @PostMapping("/service/{requestId}/inventory-items")
    public ResponseEntity<Map<String, Object>> addInventoryItems(
            @PathVariable Integer requestId,
            @RequestBody List<Map<String, Object>> items,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for add inventory items request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Prepare the request body with the list of items
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("items", items);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/" + requestId + "/inventory-items",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyMap());
            }
        } catch (Exception e) {
            log.error("Error adding inventory items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Add labor charges to a service request
     */
    @PostMapping("/service/{requestId}/labor-charges")
    public ResponseEntity<Map<String, Object>> addLaborCharges(
            @PathVariable Integer requestId,
            @RequestBody List<Map<String, Object>> laborCharges,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for add labor charges request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(laborCharges, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/" + requestId + "/labor-charges",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyMap());
            }
        } catch (Exception e) {
            log.error("Error adding labor charges: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Update service request status
     */
    @PutMapping("/service/{requestId}/status")
    public ResponseEntity<Map<String, Object>> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> statusUpdate,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for status update request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(statusUpdate, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyMap());
            }
        } catch (Exception e) {
            log.error("Error updating service status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Generate bill for a service request
     */
    @PostMapping("/service/{requestId}/generate-bill")
    public ResponseEntity<Map<String, Object>> generateBill(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> billRequest,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for generate bill request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(billRequest, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/" + requestId + "/generate-bill",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> result = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(result);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyMap());
            }
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Get current bill for a service request
     */
    @GetMapping("/service/{requestId}/current-bill")
    public ResponseEntity<Map<String, Object>> getCurrentBill(
            @PathVariable Integer requestId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            log.warn("No valid token found for current bill request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        try {
            HttpHeaders headers = createAuthHeaders(validToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiBaseUrl + "/service-advisor/dashboard/" + requestId + "/current-bill",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> bill = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<Map<String, Object>>() {}
                );
                return ResponseEntity.ok(bill);
            } else {
                log.warn("Unexpected response from API: {}", response.getStatusCode());
                return ResponseEntity.status(response.getStatusCode()).body(Collections.emptyMap());
            }
        } catch (Exception e) {
            log.error("Error fetching current bill: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Gets a valid token from various sources
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

    /**
     * Creates authentication headers with a token
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