package com.albany.mvc.controller.customer;

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
public class CustomerServiceController {

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerServiceController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Proxy endpoint for creating service requests
     */
    @PostMapping("/api/service-requests")
    @ResponseBody
    public ResponseEntity<?> createServiceRequest(@RequestBody Map<String, Object> requestData,
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
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestData, headers);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/service-requests",
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to create service request: " + e.getMessage()
            ));
        }
    }
}