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
public class CustomerInvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerInvoiceController.class);

    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public CustomerInvoiceController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * API proxy for getting customer invoices
     */
    @GetMapping("/api/invoices")
    @ResponseBody
    public ResponseEntity<?> getInvoices(HttpServletRequest request, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            logger.warn("Authentication required for /api/invoices");
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Cache-Control", "no-cache");
        
        try {
            logger.info("Fetching invoices from API");
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/invoices",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Object.class
            );
            
            logger.info("API response status: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching invoices: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to load invoices: " + e.getMessage()
            ));
        }
    }

    /**
     * API proxy for creating payment order for an invoice
     */
    @PostMapping("/api/invoices/{invoiceId}/payment")
    @ResponseBody
    public ResponseEntity<?> createPaymentOrder(@PathVariable Integer invoiceId,
                                              HttpServletRequest request, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            logger.warn("Authentication required for creating payment order");
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        
        try {
            logger.info("Creating payment order for invoice: {}", invoiceId);
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/invoices/" + invoiceId + "/payment",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Object.class
            );
            
            // Store the token in session for the payment form
            if (response.getStatusCode() == HttpStatus.OK) {
                session.setAttribute("authToken", token);
            }
            
            logger.info("Payment order created with status: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error creating payment order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to create payment order: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API proxy for verifying payment
     */
    @PostMapping("/api/invoices/verify-payment")
    @ResponseBody
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> paymentData,
                                         HttpServletRequest request, HttpSession session) {
        String token = (String) session.getAttribute("authToken");
        
        // Check for token in request header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            logger.warn("Authentication required for verifying payment");
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(paymentData, headers);
        
        try {
            logger.info("Verifying payment with payment data: {}", paymentData);
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/invoices/verify-payment",
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            logger.info("Payment verification response status: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to verify payment: " + e.getMessage()
            ));
        }
    }
}