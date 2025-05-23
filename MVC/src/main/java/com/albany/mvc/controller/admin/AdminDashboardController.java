package com.albany.mvc.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Controller for dashboard API endpoints only
 * No view mapping - that's handled by AdminController
 */
@Controller
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminDashboardController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Get dashboard data from the REST API
     */
    @GetMapping("/api/data")
    @ResponseBody
    public ResponseEntity<?> getDashboardData(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/dashboard/api/data",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Error fetching dashboard data: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Failed to fetch dashboard data: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching dashboard data: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard data: " + e.getMessage()));
        }
    }

    /**
     * Assign service advisor to a request
     */
    @PutMapping("/api/assign/{requestId}")
    @ResponseBody
    public ResponseEntity<?> assignAdvisor(
            @PathVariable Integer requestId,
            @RequestParam Integer advisorId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/dashboard/api/assign/" + requestId + "?advisorId=" + advisorId,
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Error assigning advisor: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Failed to assign advisor: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error assigning advisor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to assign advisor: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create HTTP headers with authorization if provided
     */
    private HttpHeaders createHeaders(String authHeader, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set("Authorization", authHeader);
        } else if (token != null && !token.isEmpty()) {
            headers.set("Authorization", "Bearer " + token);
        }
        return headers;
    }
}