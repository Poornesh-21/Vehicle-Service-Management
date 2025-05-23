// MVC/src/main/java/com/albany/mvc/controller/admin/AdminCompletedServiceController.java
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
public class AdminCompletedServiceController {
    private static final Logger logger = LoggerFactory.getLogger(AdminCompletedServiceController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminCompletedServiceController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Renders the completed services page
     */
    @GetMapping("/completed-services")
    public String completedServicesPage(
            @RequestParam(required = false) String token,
            @RequestParam(required = false) String success,
            Model model) {

        if (token == null || token.isEmpty()) {
            return "redirect:/admin/login?error=session_expired";
        }

        model.addAttribute("token", token);
        if (success != null) {
            model.addAttribute("success", success);
        }

        return "admin/completed_services";
    }

    /**
     * Get all completed services
     */
    @GetMapping("/api/completed-services")
    @ResponseBody
    public ResponseEntity<List<?>> getAllCompletedServices(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<?>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/completed-services",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<?>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching completed services: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Get completed service by ID
     */
    @GetMapping("/api/completed-services/{id}")
    @ResponseBody
    public ResponseEntity<?> getCompletedServiceById(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/completed-services/" + id,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching completed service {}: {}", id, e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Error fetching completed service details"));
            }
        } catch (Exception e) {
            logger.error("Error fetching completed service {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching completed service details"));
        }
    }

    /**
     * Get invoice details for a completed service
     */
    @GetMapping("/api/completed-services/{id}/invoice-details")
    @ResponseBody
    public ResponseEntity<?> getInvoiceDetails(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/completed-services/" + id + "/invoice-details",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching invoice details for service {}: {}", id, e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Error fetching invoice details"));
            }
        } catch (Exception e) {
            logger.error("Error fetching invoice details for service {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching invoice details"));
        }
    }

    /**
     * Generate invoice for a completed service
     */
    @PostMapping("/api/invoices/service-request/{id}/generate")
    @ResponseBody
    public ResponseEntity<?> generateInvoice(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/invoices/service-request/" + id + "/generate",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error generating invoice for service {}: {}", id, e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Error generating invoice"));
            }
        } catch (Exception e) {
            logger.error("Error generating invoice for service {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error generating invoice"));
        }
    }

    /**
     * Process payment for a completed service
     */
    @PostMapping("/api/completed-services/{id}/payment")
    @ResponseBody
    public ResponseEntity<?> processPayment(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/completed-services/" + id + "/payment",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error processing payment for service {}: {}", id, e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Error processing payment"));
            }
        } catch (Exception e) {
            logger.error("Error processing payment for service {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing payment"));
        }
    }

    /**
     * Mark service as delivered
     */
    @PostMapping("/api/completed-services/{id}/dispatch")
    @ResponseBody
    public ResponseEntity<?> markAsDelivered(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/completed-services/" + id + "/dispatch",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error marking service as delivered {}: {}", id, e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("error", "Error marking service as delivered"));
            }
        } catch (Exception e) {
            logger.error("Error marking service as delivered {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error marking service as delivered"));
        }
    }

    /**
     * Download invoice
     */
    @GetMapping("/api/completed-services/{id}/invoice/download")
    public ResponseEntity<?> downloadInvoice(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        try {
            HttpHeaders headers = createHeaders(authHeader, token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/api/completed-services/" + id + "/invoice/download",
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.APPLICATION_PDF);
            responseHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice-" + id + ".pdf");

            return new ResponseEntity<>(response.getBody(), responseHeaders, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error downloading invoice for service {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error downloading invoice"));
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