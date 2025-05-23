package com.albany.mvc.controller.serviceAdvisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/serviceAdvisor/api")
public class ServiceAdvisorAPIController {

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServiceAdvisorAPIController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/assigned-vehicles")
    public ResponseEntity<List<?>> getAssignedVehicles(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<?>> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/api/assigned-vehicles",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<?>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @GetMapping("/service-details/{requestId}")
    public ResponseEntity<?> getServiceDetails(
            @PathVariable Integer requestId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/api/service-details/" + requestId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Failed to fetch service details"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @GetMapping("/inventory-items")
    public ResponseEntity<List<?>> getInventoryItems(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<?>> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/api/inventory-items",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<?>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @PutMapping("/service/{requestId}/status")
    public ResponseEntity<?> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> statusUpdate,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(statusUpdate, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/api/service/" + requestId + "/status",
                    HttpMethod.PUT,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Failed to update status"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/service/{requestId}/labor-charges")
    public ResponseEntity<?> updateLaborCharges(
            @PathVariable Integer requestId,
            @RequestBody List<Map<String, Object>> laborCharges,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(laborCharges, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/api/service/" + requestId + "/labor-charges",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Failed to update labor charges"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    @PostMapping("/service/{requestId}/inventory-items")
    public ResponseEntity<?> updateInventoryItems(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> materialRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(materialRequest, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/serviceAdvisor/api/service/" + requestId + "/inventory-items",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", "Failed to update inventory items"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
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