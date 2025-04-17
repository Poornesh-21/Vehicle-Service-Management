package com.albany.mvc.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final RestTemplate restTemplate;

    @Value("${api.base-url}")
    private String apiBaseUrl;

    // Main inventory page
    @GetMapping
    public String inventoryPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        log.info("Accessing inventory management page");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/admin/login?error=session_expired";
        }

        // Set the admin's name for the page
        model.addAttribute("userName", "Arthur Morgan");

        return "admin/inventory";
    }

    // API endpoint for getting all inventory items
    @GetMapping("/api/items")
    @ResponseBody
    public ResponseEntity<?> getAllInventoryItems(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();

            // Enhanced logging & handling
            log.debug("Token value first 10 chars: {}",
                    validToken.substring(0, Math.min(10, validToken.length())));

            // Direct header setting with bearer prefix
            headers.set("Authorization", "Bearer " + validToken);

            log.debug("Request headers: {}", headers);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED URL Construction - removing duplicate /api segment
            String fullUrl = apiBaseUrl.replace("/api", "") + "/admin/inventory";
            log.debug("Making request to: {}", fullUrl);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            log.debug("API response for inventory items: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("403 Forbidden response from API. Token might be invalid or expired.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Your session may have expired."));
        } catch (Exception e) {
            log.error("Error fetching inventory items: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch inventory items: " + e.getMessage()));
        }
    }

    // API endpoint for getting inventory stats
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getInventoryStats(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED URL Construction
            String fullUrl = apiBaseUrl.replace("/api", "") + "/admin/inventory/stats";
            log.debug("Making request to: {}", fullUrl);

            ResponseEntity<Map<String, Long>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Long>>() {}
            );

            log.debug("API response for inventory stats: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("403 Forbidden response from API. Token might be invalid or expired.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Your session may have expired."));
        } catch (Exception e) {
            log.error("Error fetching inventory stats: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch inventory statistics: " + e.getMessage()));
        }
    }

    // API endpoint for getting item details
    @GetMapping("/api/items/{id}")
    @ResponseBody
    public ResponseEntity<?> getInventoryItemById(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED URL Construction
            String fullUrl = apiBaseUrl.replace("/api", "") + "/admin/inventory/" + id;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.debug("API response for inventory item: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("403 Forbidden response from API. Token might be invalid or expired.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Your session may have expired."));
        } catch (Exception e) {
            log.error("Error fetching inventory item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch inventory item: " + e.getMessage()));
        }
    }

    // API endpoint for getting item usage history
    @GetMapping("/api/items/{id}/usage-history")
    @ResponseBody
    public ResponseEntity<?> getItemUsageHistory(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // FIXED URL Construction
            String fullUrl = apiBaseUrl.replace("/api", "") + "/admin/inventory/" + id + "/usage-history";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            log.debug("API response for usage history: {}", response.getStatusCode());
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("403 Forbidden response from API. Token might be invalid or expired.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Your session may have expired."));
        } catch (Exception e) {
            log.error("Error fetching usage history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch usage history: " + e.getMessage()));
        }
    }

    // API endpoint for creating an inventory item
    @PostMapping("/api/items")
    @ResponseBody
    public ResponseEntity<?> createInventoryItem(
            @RequestBody Map<String, Object> itemData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        // Get token from various sources
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + validToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(itemData, headers);

            // FIXED URL Construction
            String fullUrl = apiBaseUrl.replace("/api", "") + "/admin/inventory";
            log.debug("Making POST request to: {}", fullUrl);
            log.debug("Request body: {}", itemData);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            log.info("Inventory item created successfully");
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (HttpClientErrorException.Forbidden e) {
            log.error("403 Forbidden response from API. Token might be invalid or expired.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. Your session may have expired."));
        } catch (Exception e) {
            log.error("Error creating inventory item: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create inventory item: " + e.getMessage()));
        }
    }

    // Continue with other methods...

    /**
     * Gets a valid token from various sources
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }

    /**
     * Gets a valid token from various sources with Auth header
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            // Store token in session
            HttpSession session = request.getSession();
            session.setAttribute("jwt-token", tokenParam);
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