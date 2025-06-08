package com.albany.mvc.controller.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/customer")
public class CustomerVehicleController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerVehicleController.class);

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerVehicleController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Proxy endpoint for getting customer vehicles
     */
    @GetMapping("/api/vehicles")
    @ResponseBody
    public ResponseEntity<?> getCustomerVehicles(HttpServletRequest request, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/vehicles",
                HttpMethod.GET,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to load vehicles: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to load vehicles: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Proxy endpoint for adding a new vehicle
     */
    @PostMapping("/api/vehicles/add")
    @ResponseBody
    public ResponseEntity<?> addVehicle(@RequestBody Map<String, Object> vehicleData,
                                        HttpServletRequest request, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vehicleData, headers);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/vehicles/add",
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to add vehicle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to add vehicle: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Proxy endpoint for updating a vehicle
     */
    @PutMapping("/api/vehicles/update/{vehicleId}")
    @ResponseBody
    public ResponseEntity<?> updateVehicle(@PathVariable Integer vehicleId,
                                        @RequestBody Map<String, Object> vehicleData,
                                        HttpServletRequest request, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(vehicleData, headers);
        
        try {
            logger.info("Forwarding vehicle update request to REST API for vehicle ID: {}", vehicleId);
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/vehicles/update/" + vehicleId,
                HttpMethod.PUT,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to update vehicle {}: {}", vehicleId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to update vehicle: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Proxy endpoint for deleting a vehicle
     */
    @DeleteMapping("/api/vehicles/delete/{vehicleId}")
    @ResponseBody
    public ResponseEntity<?> deleteVehicle(@PathVariable Integer vehicleId,
                                        HttpServletRequest request, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            logger.info("Forwarding vehicle delete request to REST API for vehicle ID: {}", vehicleId);
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/vehicles/delete/" + vehicleId,
                HttpMethod.DELETE,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Failed to delete vehicle {}: {}", vehicleId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to delete vehicle: " + e.getMessage()
            ));
        }
    }
}