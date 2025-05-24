package com.albany.mvc.controller.admin;

import com.albany.mvc.dto.InventoryItemDTO;
import com.albany.mvc.dto.InventoryStatsDTO;
import com.albany.mvc.dto.MaterialUsageDTO;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminInventoryController {
    private static final Logger logger = LoggerFactory.getLogger(AdminInventoryController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.base.url:http://localhost:8080}")
    private String apiBaseUrl;

    private final RestTemplate restTemplate;

    public AdminInventoryController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Renders the inventory page
     * Removed token parameter requirement - authentication handled by JavaScript
     */
    @GetMapping("/inventory")
    public String inventoryPage(
            @RequestParam(required = false) String success,
            Model model) {

        // Token check removed - handled by client-side JavaScript

        if (success != null) {
            model.addAttribute("success", success);
        }

        return "admin/inventory";
    }

    /**
     * Get all inventory items
     */
    @GetMapping("/inventory/api/items")
    @ResponseBody
    public ResponseEntity<List<InventoryItemDTO>> getAllInventoryItems(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<InventoryItemDTO>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/inventory/api/items",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<InventoryItemDTO>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching inventory items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * Get inventory item by ID
     */
    @GetMapping("/inventory/api/items/{id}")
    @ResponseBody
    public ResponseEntity<InventoryItemDTO> getInventoryItemById(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<InventoryItemDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/inventory/api/items/" + id,
                    HttpMethod.GET,
                    entity,
                    InventoryItemDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching inventory item: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            logger.error("Error fetching inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create inventory item
     */
    @PostMapping("/inventory")
    @ResponseBody
    public ResponseEntity<?> createInventoryItem(
            @RequestBody InventoryItemDTO itemDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<InventoryItemDTO> entity = new HttpEntity<>(itemDTO, headers);

            ResponseEntity<InventoryItemDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/inventory",
                    HttpMethod.POST,
                    entity,
                    InventoryItemDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error creating inventory item: {}", e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("message", "An error occurred while creating the inventory item"));
            }
        } catch (Exception e) {
            logger.error("Error creating inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while creating the inventory item"));
        }
    }

    /**
     * Update inventory item
     */
    @PutMapping("/inventory/{id}")
    @ResponseBody
    public ResponseEntity<?> updateInventoryItem(
            @PathVariable("id") Integer id,
            @RequestBody InventoryItemDTO itemDTO,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<InventoryItemDTO> entity = new HttpEntity<>(itemDTO, headers);

            ResponseEntity<InventoryItemDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/inventory/" + id,
                    HttpMethod.PUT,
                    entity,
                    InventoryItemDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error updating inventory item: {}", e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("message", "An error occurred while updating the inventory item"));
            }
        } catch (Exception e) {
            logger.error("Error updating inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while updating the inventory item"));
        }
    }

    /**
     * Delete inventory item
     */
    @DeleteMapping("/inventory/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteInventoryItem(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/inventory/" + id,
                    HttpMethod.DELETE,
                    entity,
                    Map.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error deleting inventory item: {}", e.getMessage());
            try {
                String errorBody = e.getResponseBodyAsString();
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                return ResponseEntity.status(e.getStatusCode()).body(errorMap);
            } catch (Exception ex) {
                return ResponseEntity.status(e.getStatusCode())
                        .body(Map.of("message", "An error occurred while deleting the inventory item"));
            }
        } catch (Exception e) {
            logger.error("Error deleting inventory item", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while deleting the inventory item"));
        }
    }

    /**
     * Get inventory stats
     */
    @GetMapping("/inventory/api/stats")
    @ResponseBody
    public ResponseEntity<InventoryStatsDTO> getInventoryStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<InventoryStatsDTO> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/inventory/api/stats",
                    HttpMethod.GET,
                    entity,
                    InventoryStatsDTO.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            logger.error("Error fetching inventory stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get usage history for an item
     */
    @GetMapping("/inventory/api/items/{id}/usage-history")
    @ResponseBody
    public ResponseEntity<List<MaterialUsageDTO>> getItemUsageHistory(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            HttpHeaders headers = createHeaders(authHeader);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<MaterialUsageDTO>> response = restTemplate.exchange(
                    apiBaseUrl + "/admin/inventory/api/items/" + id + "/usage-history",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<MaterialUsageDTO>>() {}
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            logger.error("Client error fetching usage history: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Collections.emptyList());
        } catch (HttpServerErrorException e) {
            logger.error("Server error fetching usage history: {}", e.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Error fetching usage history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
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