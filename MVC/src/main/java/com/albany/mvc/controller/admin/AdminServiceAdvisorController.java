package com.albany.mvc.controller.admin;

import com.albany.mvc.dto.ServiceAdvisorDTO;
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
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminServiceAdvisorController {
    private static final Logger logger = LoggerFactory.getLogger(AdminServiceAdvisorController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminServiceAdvisorController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Renders the service advisors page
     * Removed token parameter requirement - authentication handled by JavaScript
     */
    @GetMapping("/service-advisors")
    public String serviceAdvisorsPage(
            @RequestParam(required = false) String success,
            Model model) {

        // Token check removed - handled by client-side JavaScript

        if (success != null) {
            model.addAttribute("success", success);
        }

        return "admin/serviceAdvisor";
    }

    /**
     * Get all service advisors
     */
    @GetMapping("/service-advisors/api/advisors")
    @ResponseBody
    public ResponseEntity<List<ServiceAdvisorDTO>> getAllServiceAdvisors(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<ServiceAdvisorDTO>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-advisors/api/advisors",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<ServiceAdvisorDTO>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching service advisors: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (HttpServerErrorException e) {
            logger.error("Server error fetching service advisors: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error fetching service advisors: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }
    
    /**
     * Get service advisor by ID
     */
    @GetMapping("/service-advisors/{id}")
    @ResponseBody
    public ResponseEntity<ServiceAdvisorDTO> getServiceAdvisorById(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<ServiceAdvisorDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-advisors/" + id,
                    HttpMethod.GET,
                    entity,
                    ServiceAdvisorDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching service advisor {}: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            logger.error("Error fetching service advisor {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Creates a new service advisor through the REST API
     */
    @PostMapping("/service-advisors")
    @ResponseBody
    public ResponseEntity<?> createServiceAdvisor(
            @RequestBody ServiceAdvisorDTO advisorDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceAdvisorDTO> entity = new HttpEntity<>(advisorDTO, headers);

            ResponseEntity<ServiceAdvisorDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-advisors",
                    HttpMethod.POST,
                    entity,
                    ServiceAdvisorDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error creating service advisor: {}", e.getMessage());

            // Try to extract error message from response
            try {
                String errorBody = e.getResponseBodyAsString();
                logger.error("Error response body: {}", errorBody);

                // Try to parse as JSON
                try {
                    Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                    String errorMessage = errorMap.containsKey("message") ?
                            errorMap.get("message").toString() :
                            "Validation failed. Please check your input.";

                    return ResponseEntity.status(e.getStatusCode())
                            .body(Collections.singletonMap("message", errorMessage));
                } catch (Exception ex) {
                    // If not JSON, return the raw error
                    return ResponseEntity.status(e.getStatusCode())
                            .body(Collections.singletonMap("message", errorBody));
                }
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Collections.singletonMap("message", "An error occurred while creating the service advisor."));
            }
        } catch (Exception e) {
            logger.error("Error creating service advisor: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while creating the service advisor."));
        }
    }

    /**
     * Updates an existing service advisor through the REST API
     */
    @PutMapping("/service-advisors/{id}")
    @ResponseBody
    public ResponseEntity<?> updateServiceAdvisor(
            @PathVariable("id") Integer id,
            @RequestBody ServiceAdvisorDTO advisorDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ServiceAdvisorDTO> entity = new HttpEntity<>(advisorDTO, headers);

            ResponseEntity<ServiceAdvisorDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/service-advisors/" + id,
                    HttpMethod.PUT,
                    entity,
                    ServiceAdvisorDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error updating service advisor {}: {}", id, e.getMessage());

            // Try to extract error message from response
            try {
                String errorBody = e.getResponseBodyAsString();
                logger.error("Error response body: {}", errorBody);

                // Try to parse as JSON
                try {
                    Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                    String errorMessage = errorMap.containsKey("message") ?
                            errorMap.get("message").toString() :
                            "Validation failed. Please check your input.";

                    return ResponseEntity.status(e.getStatusCode())
                            .body(Collections.singletonMap("message", errorMessage));
                } catch (Exception ex) {
                    // If not JSON, return the raw error
                    return ResponseEntity.status(e.getStatusCode())
                            .body(Collections.singletonMap("message", errorBody));
                }
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Collections.singletonMap("message", "An error occurred while updating the service advisor."));
            }
        } catch (Exception e) {
            logger.error("Error updating service advisor {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while updating the service advisor."));
        }
    }

    /**
     * Helper method to create HTTP headers with authorization if provided
     * Removed token parameter since we only use Authorization header now
     */
    private HttpHeaders createHeaders(String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        if (authHeader != null && !authHeader.isEmpty()) {
            headers.set("Authorization", authHeader);
        }
        return headers;
    }
}