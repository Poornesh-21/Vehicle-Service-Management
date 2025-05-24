package com.albany.mvc.controller.admin;

import com.albany.mvc.dto.CustomerDTO;
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

/**
 * Controller for customer management - handles both page rendering
 * and API operations for customers.
 */
@Controller
@RequestMapping("/admin")
public class AdminCustomerController {
    private static final Logger logger = LoggerFactory.getLogger(AdminCustomerController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminCustomerController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Renders the customers page
     * Removed token parameter - authentication handled by JavaScript
     */
    @GetMapping("/customers")
    public String customersPage(
            @RequestParam(required = false) String success,
            Model model) {

        // Token check removed - handled by client-side JavaScript

        if (success != null) {
            model.addAttribute("success", success);
        }

        return "admin/customers";
    }

    /* API ENDPOINTS */

    /**
     * Fetches all customers from the REST API
     */
    @GetMapping("/customers/api")
    @ResponseBody
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CustomerDTO>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/customers/api",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<CustomerDTO>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching customers: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (HttpServerErrorException e) {
            logger.error("Server error fetching customers: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error fetching customers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Fetches a single customer by ID from the REST API
     */
    @GetMapping("/customers/api/{id}")
    @ResponseBody
    public ResponseEntity<CustomerDTO> getCustomerById(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<CustomerDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/customers/api/" + id,
                    HttpMethod.GET,
                    entity,
                    CustomerDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            logger.error("Error fetching customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Creates a new customer through the REST API
     */
    @PostMapping("/customers/api")
    @ResponseBody
    public ResponseEntity<?> createCustomer(
            @RequestBody CustomerDTO customerDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        // Always set isActive to true for new customers
        customerDTO.setActive(true);

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CustomerDTO> entity = new HttpEntity<>(customerDTO, headers);

            ResponseEntity<CustomerDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/customers/api",
                    HttpMethod.POST,
                    entity,
                    CustomerDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error creating customer: {}", e.getMessage());

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
                        .body(Collections.singletonMap("message", "An error occurred while creating the customer."));
            }
        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while creating the customer."));
        }
    }

    /**
     * Updates an existing customer through the REST API
     */
    @PutMapping("/customers/api/{id}")
    @ResponseBody
    public ResponseEntity<?> updateCustomer(
            @PathVariable("id") Integer id,
            @RequestBody CustomerDTO customerDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<CustomerDTO> entity = new HttpEntity<>(customerDTO, headers);

            ResponseEntity<CustomerDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/customers/api/" + id,
                    HttpMethod.PUT,
                    entity,
                    CustomerDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error updating customer {}: {}", id, e.getMessage());

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
                        .body(Collections.singletonMap("message", "An error occurred while updating the customer."));
            }
        } catch (Exception e) {
            logger.error("Error updating customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "An error occurred while updating the customer."));
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