package com.albany.mvc.controller.admin;

import com.albany.mvc.dto.ServiceRequestDTO;
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

/**
 * Controller for service request management
 * Handles both page rendering and API operations for service requests
 */
@Controller
@RequestMapping("/admin")
public class AdminServiceRequestController {
    private static final Logger logger = LoggerFactory.getLogger(AdminServiceRequestController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminServiceRequestController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Renders the service requests page
     * No longer requires token as URL parameter - will be handled by JavaScript
     */
    @GetMapping("/service-requests")
    public String serviceRequestsPage(
            @RequestParam(required = false) String success,
            Model model) {

        if (success != null) {
            model.addAttribute("success", success);
        }

        return "admin/serviceRequests";
    }

    /* API ENDPOINTS */

    /**
     * Get all service requests
     */
    @GetMapping("/service-requests/api")
    @ResponseBody
    public ResponseEntity<List<ServiceRequestDTO>> getAllServiceRequests(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<ServiceRequestDTO>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-requests/api",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ServiceRequestDTO>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching service requests: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error fetching service requests: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Get service request by ID
     */
    @GetMapping("/service-requests/api/{id}")
    @ResponseBody
    public ResponseEntity<ServiceRequestDTO> getServiceRequestById(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<ServiceRequestDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-requests/api/" + id,
                    HttpMethod.GET,
                    entity,
                    ServiceRequestDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching service request {}: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            logger.error("Error fetching service request {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new service request
     */
    @PostMapping("/service-requests/api")
    @ResponseBody
    public ResponseEntity<?> createServiceRequest(
            @RequestBody ServiceRequestDTO requestDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceRequestDTO> entity = new HttpEntity<>(requestDTO, headers);

            ResponseEntity<ServiceRequestDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-requests/api",
                    HttpMethod.POST,
                    entity,
                    ServiceRequestDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error creating service request: {}", e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode())
                        .body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Failed to create service request"));
            }
        } catch (Exception e) {
            logger.error("Error creating service request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create service request"));
        }
    }

    /**
     * Assign a service advisor to a request
     */
    @PutMapping("/service-requests/api/{id}/assign")
    @ResponseBody
    public ResponseEntity<?> assignServiceAdvisor(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Integer> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ServiceRequestDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-requests/api/" + id + "/assign",
                    HttpMethod.PUT,
                    entity,
                    ServiceRequestDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error assigning service advisor: {}", e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Failed to assign service advisor"));
            }
        } catch (Exception e) {
            logger.error("Error assigning service advisor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to assign service advisor"));
        }
    }

    /**
     * Helper method to create HTTP headers with authorization if provided
     */
    private HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set("Authorization", authHeader);
        }
        return headers;
    }
}