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
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required"
            ));
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        
        try {
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
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error creating payment order: {}", e.getMessage());
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
            ResponseEntity<Object> response = restTemplate.exchange(
                apiBaseUrl + "/api/customer/invoices/verify-payment",
                HttpMethod.POST,
                entity,
                Object.class
            );
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error verifying payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to verify payment: " + e.getMessage()
            ));
        }
    }
}