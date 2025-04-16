package com.albany.mvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    /**
     * Get all vehicles for a customer
     */
    @GetMapping("/customers/{customerId}/vehicles")
    public ResponseEntity<?> getVehiclesForCustomer(
            @PathVariable Integer customerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            // Set up authentication
            String finalToken = getToken(authHeader, token);
            if (finalToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + finalToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED: Correct API URL - remove duplicate paths
            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId + "/vehicles",
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            log.debug("API response for customer's vehicles: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error fetching vehicles for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch vehicles: " + e.getMessage()));
        }
    }

    /**
     * Create a new vehicle for a customer
     */
    @PostMapping("/customers/{customerId}/vehicles")
    public ResponseEntity<?> createVehicleForCustomer(
            @PathVariable Integer customerId,
            @RequestBody Map<String, Object> vehicleData,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        log.info("Creating vehicle for customer {}: {}", customerId, vehicleData);

        try {
            // Set up authentication
            String finalToken = getToken(authHeader, token);
            if (finalToken == null) {
                log.error("No valid token found for authorization");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            HttpHeaders headers = new HttpHeaders();
            // Ensure the "Bearer " prefix is included
            headers.set("Authorization", finalToken.startsWith("Bearer ") ?
                    finalToken : "Bearer " + finalToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Debug logging
            log.debug("Sending request to API with token: {}",
                    finalToken.substring(0, Math.min(10, finalToken.length())) + "...");
            log.debug("Request headers: {}", headers);

            // Make sure customerId is included in the request body
            vehicleData.put("customerId", customerId);
            log.debug("Request body: {}", vehicleData);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vehicleData, headers);

            // FIXED: Correct API URL - remove duplicate paths
            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/customers/" + customerId + "/vehicles",
                    HttpMethod.POST,
                    entity,
                    Object.class
            );

            log.info("Vehicle created successfully for customer {}", customerId);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error creating vehicle for customer {}: {}", customerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create vehicle: " + e.getMessage()));
        }
    }

    // Similarly, fix all other methods in this controller...
    @GetMapping("/vehicles/{id}")
    public ResponseEntity<?> getVehicleById(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        log.info("Fetching vehicle with ID: {}", id);

        try {
            // Set up authentication
            String finalToken = getToken(authHeader, token);
            if (finalToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + finalToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED: Correct API URL - remove duplicate paths
            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicles/" + id,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );

            log.debug("Vehicle API response status: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error fetching vehicle details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch vehicle details: " + e.getMessage()));
        }
    }

    @PutMapping("/vehicles/{id}")
    public ResponseEntity<?> updateVehicle(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> vehicleData,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        log.info("Updating vehicle with ID: {}", id);

        try {
            // Set up authentication
            String finalToken = getToken(authHeader, token);
            if (finalToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + finalToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vehicleData, headers);

            // FIXED: Correct API URL - remove duplicate paths
            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicles/" + id,
                    HttpMethod.PUT,
                    entity,
                    Object.class
            );

            log.info("Vehicle updated successfully");
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error updating vehicle: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update vehicle: " + e.getMessage()));
        }
    }

    @DeleteMapping("/vehicles/{id}")
    public ResponseEntity<?> deleteVehicle(
            @PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        log.info("Deleting vehicle with ID: {}", id);

        try {
            // Set up authentication
            String finalToken = getToken(authHeader, token);
            if (finalToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + finalToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED: Correct API URL - remove duplicate paths
            ResponseEntity<Object> response = restTemplate.exchange(
                    apiBaseUrl + "/vehicles/" + id,
                    HttpMethod.DELETE,
                    entity,
                    Object.class
            );

            log.info("Vehicle deleted successfully");
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            log.error("Error deleting vehicle: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete vehicle: " + e.getMessage()));
        }
    }

    /**
     * Helper method to get token from various sources
     */
    private String getToken(String authHeader, String tokenParam) {
        // Check token parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        // Check authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // No valid token found
        return null;
    }
}