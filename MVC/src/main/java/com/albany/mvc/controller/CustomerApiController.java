package com.albany.mvc.controller;

import com.albany.mvc.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/customers/api")
@RequiredArgsConstructor
@Slf4j
public class CustomerApiController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Map<String, Object>> customers = customerService.getAllCustomers(validToken);
            return ResponseEntity.ok(customers);
        } catch (Exception e) {
            log.error("Error fetching customers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCustomerById(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> customer = customerService.getCustomerById(id, validToken);
            if (customer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(customer);
        } catch (Exception e) {
            log.error("Error fetching customer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyMap());
        }
    }

    @PostMapping
    public ResponseEntity<?> createCustomer(
            @RequestBody Map<String, Object> customerData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> createdCustomer = customerService.createCustomer(customerData, validToken);
            return ResponseEntity.ok(createdCustomer);
        } catch (Exception e) {
            log.error("Error creating customer: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> customerData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> updatedCustomer = customerService.updateCustomer(id, customerData, validToken);
            if (updatedCustomer.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedCustomer);
        } catch (Exception e) {
            log.error("Error updating customer: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get valid token
        String validToken = getValidToken(token, authHeader, request);
        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            boolean deleted = customerService.deleteCustomer(id, validToken);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting customer: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Helper method to get a valid token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
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
}