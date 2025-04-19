package com.albany.mvc.controller;

import com.albany.mvc.service.VehicleTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingController {

    private final VehicleTrackingService vehicleTrackingService;

    /**
     * Main vehicles page
     */
    @GetMapping
    public String vehiclesPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {

        log.info("Accessing vehicles page");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/admin/login?error=session_expired";
        }

        // Set the admin's name for the page (in a real app, get from user session or JWT)
        model.addAttribute("userName", "Arthur Morgan");

        return "admin/vehicles";
    }

    /**
     * API endpoint to get vehicles under service
     */
    @GetMapping("/api/under-service")
    @ResponseBody
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
    @GetMapping("/api/completed-services")
    @ResponseBody
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
    @GetMapping("/api/service-request/{id}")
    @ResponseBody
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
    @PutMapping("/api/service-request/{id}/status")
    @ResponseBody
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
    @PostMapping("/api/service-request/{id}/payment")
    @ResponseBody
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

        boolean recorded = vehicleTrackingService.recordPayment(id, paymentDetails, validToken);
        
        if (recorded) {
            return ResponseEntity.ok(Collections.singletonMap("message", "Payment recorded successfully"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "Failed to record payment"));
        }
    }

    /**
     * API endpoint to generate invoice
     */
    @PostMapping("/api/service-request/{id}/invoice")
    @ResponseBody
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

        boolean generated = vehicleTrackingService.generateInvoice(id, invoiceDetails, validToken);
        
        if (generated) {
            return ResponseEntity.ok(Collections.singletonMap("message", "Invoice generated successfully"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "Failed to generate invoice"));
        }
    }

    /**
     * API endpoint to dispatch vehicle
     */
    @PostMapping("/api/service-request/{id}/dispatch")
    @ResponseBody
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

        boolean dispatched = vehicleTrackingService.dispatchVehicle(id, dispatchDetails, validToken);
        
        if (dispatched) {
            return ResponseEntity.ok(Collections.singletonMap("message", "Vehicle dispatched successfully"));
        } else {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "Failed to dispatch vehicle"));
        }
    }

    /**
     * API endpoint to filter vehicles under service
     */
    @PostMapping("/api/under-service/filter")
    @ResponseBody
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
    @PostMapping("/api/completed-services/filter")
    @ResponseBody
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
    @GetMapping("/api/search")
    @ResponseBody
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