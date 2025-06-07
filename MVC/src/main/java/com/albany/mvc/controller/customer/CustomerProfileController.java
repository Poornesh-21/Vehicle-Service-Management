package com.albany.mvc.controller.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
@RequestMapping("/customer")
public class CustomerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProfileController.class);

    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerProfileController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Display customer profile page
     */
    @GetMapping("/profile")
    public String profilePage(Model model, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        if (token == null || token.isEmpty()) {
            return "redirect:/authentication/login?message=Please login to view your profile&type=info&redirect=/customer/profile";
        }
        
        try {
            // Fetch user data to check authentication
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/api/customer/profile",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                model.addAttribute("user", response.getBody());
            }
            
            return "customer/profile";
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // Clear invalid session token
                session.removeAttribute("authToken");
                return "redirect:/authentication/login?message=Your session has expired. Please login again.&type=error&redirect=/customer/profile";
            }
            
            logger.error("Error fetching user data: {}", e.getMessage());
            return "customer/profile"; // Still show the page, client-side JS will handle authentication
        }
    }

    /**
     * API proxy for profile data
     */
    @GetMapping("/api/profile")
    @ResponseBody
    public ResponseEntity<?> getProfile(HttpServletRequest request, HttpSession session) {
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
                apiBaseUrl + "/api/customer/profile",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to load profile: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API proxy for updating profile
     */
    @PostMapping("/api/profile/update")
    @ResponseBody
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, Object> profileData,
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
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        try {
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/profile",
                HttpMethod.PUT,
                new HttpEntity<>(profileData, headers),
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to update profile: " + e.getMessage()
            ));
        }
    }
}