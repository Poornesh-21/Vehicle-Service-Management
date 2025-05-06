package com.albany.mvc.controller;

import com.albany.mvc.dto.ServiceRequestDto;
import com.albany.mvc.service.ServiceAssignmentService;
import com.albany.mvc.service.ServiceRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/serviceAdvisor")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardController {

    private final ServiceAssignmentService assignmentService;
    private final ServiceRequestService serviceRequestService;

    /**
     * Dashboard page rendering
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        log.info("Accessing service advisor dashboard");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/serviceAdvisor/login?error=session_expired";
        }

        // Add user information to the model
        HttpSession session = request.getSession(false);
        if (session != null) {
            String firstName = (String) session.getAttribute("firstName");
            String lastName = (String) session.getAttribute("lastName");
            if (firstName != null && lastName != null) {
                model.addAttribute("userName", firstName + " " + lastName);
            } else {
                model.addAttribute("userName", "Service Advisor");
            }
        } else {
            model.addAttribute("userName", "Service Advisor");
        }

        return "serviceAdvisor/dashboard";
    }

    /**
     * REST endpoint to get new service requests
     */
    @GetMapping("/api/new-assignments")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getNewAssignments(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<Map<String, Object>> newRequests = assignmentService.getNewServiceRequests(validToken);
            return ResponseEntity.ok(newRequests);
        } catch (Exception e) {
            log.error("Error fetching new service requests: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * REST endpoint to get assigned services (in progress)
     */
    @GetMapping("/api/assigned-services")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAssignedServices(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyList());
        }

        try {
            List<Map<String, Object>> assignedServices = assignmentService.getAssignedRequests(validToken);
            return ResponseEntity.ok(assignedServices);
        } catch (Exception e) {
            log.error("Error fetching assigned services: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.emptyList());
        }
    }

    /**
     * REST endpoint to get service details
     */
    @GetMapping("/api/service-details/{requestId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServiceDetails(
            @PathVariable Integer requestId,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).body(Collections.emptyMap());
        }

        try {
            // Get the DTO from the service
            ServiceRequestDto serviceDto = serviceRequestService.getServiceRequestById(requestId, validToken);

            if (serviceDto == null) {
                return ResponseEntity.notFound().build();
            }

            // Convert the DTO to a Map explicitly
            Map<String, Object> serviceDetails = convertDtoToMap(serviceDto);

            return ResponseEntity.ok(serviceDetails);
        } catch (Exception e) {
            log.error("Error fetching service details: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    /**
     * Helper method to convert ServiceRequestDto to Map<String, Object>
     */
    private Map<String, Object> convertDtoToMap(ServiceRequestDto dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("requestId", dto.getRequestId());
        map.put("vehicleId", dto.getVehicleId());
        map.put("vehicleBrand", dto.getVehicleBrand());
        map.put("vehicleModel", dto.getVehicleModel());
        map.put("registrationNumber", dto.getRegistrationNumber());
        map.put("serviceType", dto.getServiceType());
        map.put("deliveryDate", dto.getDeliveryDate());
        map.put("additionalDescription", dto.getAdditionalDescription());
        map.put("adminId", dto.getAdminId());
        map.put("serviceAdvisorId", dto.getServiceAdvisorId());
        map.put("serviceAdvisorName", dto.getServiceAdvisorName());
        map.put("status", dto.getStatus());
        map.put("customerName", dto.getCustomerName());
        map.put("customerId", dto.getCustomerId());
        map.put("membershipStatus", dto.getMembershipStatus());
        map.put("customerEmail", dto.getCustomerEmail());
        map.put("vehicleCategory", dto.getVehicleCategory());
        map.put("vehicleName", dto.getVehicleName());

        return map;
    }

    /**
     * REST endpoint to update service status
     */
    @PutMapping("/api/update-status/{requestId}")
    @ResponseBody
    public ResponseEntity<?> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> statusUpdate,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String status = statusUpdate.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }

            // Get the DTO from the service
            ServiceRequestDto updatedRequest = serviceRequestService.updateServiceRequestStatus(requestId, status, validToken);

            if (updatedRequest == null) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to update status"));
            }

            // Convert the DTO to a Map
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Status updated successfully");
            result.put("request", convertDtoToMap(updatedRequest));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating service status: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * REST endpoint to assign a service
     */
    @PostMapping("/api/assign-service/{requestId}")
    @ResponseBody
    public ResponseEntity<?> assignService(
            @PathVariable Integer requestId,
            @RequestBody Map<String, Object> assignmentData,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Map<String, Object> result = assignmentService.assignService(requestId, assignmentData, validToken);

            if (result.containsKey("error")) {
                return ResponseEntity.badRequest().body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error assigning service: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Gets a valid token from various sources
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

    /**
     * Gets a valid token from various sources (without Auth header)
     */
    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }
}