package com.albany.mvc.controller.admin;

import com.albany.mvc.dto.VehicleDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller for vehicle management
 * Handles API operations for vehicles
 */
@Controller
@RequestMapping("/admin")
public class AdminVehicleController {
    private static final Logger logger = LoggerFactory.getLogger(AdminVehicleController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminVehicleController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get vehicles for a specific customer
     * This explicitly handles the endpoint used in JavaScript
     */
    @GetMapping("/api/customers/{customerId}/vehicles")
    @ResponseBody
    public ResponseEntity<List<VehicleDTO>> getVehiclesForCustomer(
            @PathVariable("customerId") Integer customerId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            logger.info("Fetching vehicles for customer: {}", customerId);
            
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Use the REST API endpoint
            ResponseEntity<List<VehicleDTO>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/customers/" + customerId + "/vehicles",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<VehicleDTO>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching vehicles for customer {}: {}", customerId, e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error fetching vehicles for customer {}: {}", customerId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Get vehicle by ID
     */
    @GetMapping("/api/vehicles/{id}")
    @ResponseBody
    public ResponseEntity<VehicleDTO> getVehicleById(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<VehicleDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/vehicles/" + id,
                    HttpMethod.GET,
                    entity,
                    VehicleDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching vehicle {}: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            logger.error("Error fetching vehicle {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new vehicle for a customer
     */
    @PostMapping("/api/customers/{customerId}/vehicles")
    @ResponseBody
    public ResponseEntity<?> createVehicle(
            @PathVariable("customerId") Integer customerId,
            @RequestBody VehicleDTO vehicleDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<VehicleDTO> entity = new HttpEntity<>(vehicleDTO, headers);

            ResponseEntity<VehicleDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/customers/" + customerId + "/vehicles",
                    HttpMethod.POST,
                    entity,
                    VehicleDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error creating vehicle: {}", e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Failed to create vehicle"));
            }
        } catch (Exception e) {
            logger.error("Error creating vehicle: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create vehicle"));
        }
    }

    /**
     * Helper method to create HTTP headers with authorization if provided
     */
    private HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set("Authorization", authHeader);
        }
        return headers;
    }
}