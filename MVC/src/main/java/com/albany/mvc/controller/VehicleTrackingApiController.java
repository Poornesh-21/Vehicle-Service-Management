package com.albany.mvc.controller;

import com.albany.mvc.service.VehicleTrackingService;
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
@RequestMapping("/admin/api/vehicle-tracking")
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingApiController {

    private final VehicleTrackingService vehicleTrackingService;

    /**
     * API endpoint to get vehicles under service
     */
    @GetMapping("/under-service")
    public ResponseEntity<List<Map<String, Object>>> getVehiclesUnderService(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        List<Map<String, Object>> vehicles = vehicleTrackingService.getVehiclesUnderService(validToken);
        return ResponseEntity.ok(vehicles);
    }

    /**
     * API endpoint to get completed services
     */
    @GetMapping("/completed-services")
    public ResponseEntity<List<Map<String, Object>>> getCompletedServices(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        List<Map<String, Object>> services = vehicleTrackingService.getCompletedServices(validToken);
        return ResponseEntity.ok(services);
    }

    /**
     * API endpoint to get service request details
     */
    @GetMapping("/service-request/{id}")
    public ResponseEntity<Map<String, Object>> getServiceRequestDetails(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        Map<String, Object> serviceRequest = vehicleTrackingService.getServiceRequestById(id, validToken);

        if (serviceRequest.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(serviceRequest);
    }

    /**
     * API endpoint to update service status
     */
    @PutMapping("/service-request/{id}/status")
    public ResponseEntity<?> updateServiceStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> statusUpdate,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String status = statusUpdate.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Status is required"));
        }

        boolean updated = vehicleTrackingService.updateServiceStatus(id, status, validToken);

        if (updated) {
            return ResponseEntity.ok(Collections.singletonMap("message", "Status updated successfully"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "Failed to update status"));
        }
    }

    /**
     * API endpoint to record payment
     */
    @PostMapping("/service-request/{id}/payment")
    public ResponseEntity<?> recordPayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> paymentDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> result = vehicleTrackingService.recordPayment(id, paymentDetails, validToken);

        if (result.containsKey("error")) {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", result.get("error")));
        } else {
            return ResponseEntity.ok(result);
        }
    }

    /**
     * API endpoint to generate bill
     * This is the new endpoint to handle bill generation
     */
    @PostMapping("/service-request/{id}/bill")
    public ResponseEntity<?> generateBill(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> billDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Generating bill for service request {}", id);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> result = vehicleTrackingService.generateBill(id, billDetails, validToken);

            if (result.containsKey("error")) {
                return ResponseEntity.internalServerError()
                        .body(Collections.singletonMap("error", result.get("error")));
            } else {
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "Failed to generate bill: " + e.getMessage()));
        }
    }

    /**
     * API endpoint to generate invoice
     */
    @PostMapping("/service-request/{id}/invoice")
    public ResponseEntity<?> generateInvoice(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> invoiceDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> result = vehicleTrackingService.generateInvoice(id, invoiceDetails, validToken);

        if (result.containsKey("error")) {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", result.get("error")));
        } else {
            return ResponseEntity.ok(result);
        }
    }

    /**
     * API endpoint to dispatch vehicle
     */
    @PostMapping("/service-request/{id}/dispatch")
    public ResponseEntity<?> dispatchVehicle(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> dispatchDetails,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> result = vehicleTrackingService.dispatchVehicle(id, dispatchDetails, validToken);

        if (result.containsKey("error")) {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", result.get("error")));
        } else {
            return ResponseEntity.ok(result);
        }
    }

    /**
     * API endpoint to filter vehicles under service
     */
    @PostMapping("/under-service/filter")
    public ResponseEntity<List<Map<String, Object>>> filterVehiclesUnderService(
            @RequestBody Map<String, Object> filterCriteria,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        List<Map<String, Object>> filteredVehicles = vehicleTrackingService.filterVehiclesUnderService(
                filterCriteria, validToken);

        return ResponseEntity.ok(filteredVehicles);
    }

    /**
     * API endpoint to filter completed services
     */
    @PostMapping("/completed-services/filter")
    public ResponseEntity<List<Map<String, Object>>> filterCompletedServices(
            @RequestBody Map<String, Object> filterCriteria,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyList());
        }

        List<Map<String, Object>> filteredServices = vehicleTrackingService.filterCompletedServices(
                filterCriteria, validToken);

        return ResponseEntity.ok(filteredServices);
    }

    /**
     * API endpoint to search vehicles and services
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> searchVehiclesAndServices(
            @RequestParam String query,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.emptyMap());
        }

        // Create search criteria
        Map<String, Object> searchCriteria = Collections.singletonMap("search", query);

        // Get filtered results
        List<Map<String, Object>> vehiclesUnderService = vehicleTrackingService.filterVehiclesUnderService(
                searchCriteria, validToken);

        List<Map<String, Object>> completedServices = vehicleTrackingService.filterCompletedServices(
                searchCriteria, validToken);

        // Combine results
        Map<String, List<Map<String, Object>>> results = new HashMap<>();
        results.put("vehiclesUnderService", vehiclesUnderService);
        results.put("completedServices", completedServices);

        return ResponseEntity.ok(results);
    }

    /**
     * Gets a valid token from various sources with Auth header
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        // Check header next
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Check session last
        HttpSession session = request.getSession(false);
        if (session != null) {
            String sessionToken = (String) session.getAttribute("jwt-token");
            if (sessionToken != null && !sessionToken.isEmpty()) {
                return sessionToken;
            }
        }

        return null;
    }

}