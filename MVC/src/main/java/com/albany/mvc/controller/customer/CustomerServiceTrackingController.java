package com.albany.mvc.controller.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/customer")
public class CustomerServiceTrackingController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceTrackingController.class);

    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerServiceTrackingController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * API proxy for getting active services
     */
    @GetMapping("/api/services/active")
    @ResponseBody
    public ResponseEntity<?> getActiveServices(HttpServletRequest request, HttpSession session) {
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
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/service-tracking/active",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching active services: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to load active services: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API proxy for getting service tracking details
     */
    @GetMapping("/api/services/{requestId}/tracking")
    @ResponseBody
    public ResponseEntity<?> getServiceTracking(@PathVariable Integer requestId,
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
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/service-tracking/" + requestId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching service tracking: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to load service tracking: " + e.getMessage()
            ));
        }
    }
}