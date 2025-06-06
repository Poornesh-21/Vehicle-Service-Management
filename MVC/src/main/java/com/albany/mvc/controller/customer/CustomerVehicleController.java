package com.albany.mvc.controller.customer;

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
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to add vehicle: " + e.getMessage()
            ));
        }
    }
}