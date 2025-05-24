package com.albany.mvc.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/admin")
public class AdminVehicleTrackingController {
    private static final Logger logger = LoggerFactory.getLogger(AdminVehicleTrackingController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminVehicleTrackingController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/under-service")
    public String underServicePage(
            @RequestParam(required = false) String success,
            Model model) {
        if (success != null) {
            model.addAttribute("success", success);
        }
        return "admin/underservices";
    }

    @GetMapping("/api/vehicle-tracking/under-service")
    @ResponseBody
    public ResponseEntity<List<?>> getVehiclesUnderService(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<?>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/vehicle-tracking/under-service",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<?>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching vehicles under service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/api/vehicle-tracking/service-request/{id}")
    @ResponseBody
    public ResponseEntity<?> getServiceRequestDetails(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/vehicle-tracking/service-request/" + id,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching service request {}: {}", id, e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Error fetching service request details"));
            }
        } catch (Exception e) {
            logger.error("Error fetching service request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching service request details"));
        }
    }

    @PutMapping("/api/vehicle-tracking/service-request/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateServiceRequestStatus(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/vehicle-tracking/service-request/" + id + "/status",
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error updating service request status {}: {}", id, e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Error updating service request status"));
            }
        } catch (Exception e) {
            logger.error("Error updating service request status {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating service request status"));
        }
    }

    @PostMapping("/api/vehicle-tracking/under-service/filter")
    @ResponseBody
    public ResponseEntity<List<?>> filterVehiclesUnderService(
            @RequestBody Map<String, Object> filterCriteria,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(filterCriteria, headers);

            ResponseEntity<List<?>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/vehicle-tracking/under-service/filter",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<?>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error filtering vehicles under service: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    private HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set("Authorization", authHeader);
        }
        return headers;
    }
}