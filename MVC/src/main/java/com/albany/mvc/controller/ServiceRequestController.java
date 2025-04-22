package com.albany.mvc.controller;

import com.albany.mvc.dto.ServiceRequestDto;
import com.albany.mvc.service.ServiceRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @GetMapping("/admin/service-requests")
    public String serviceRequestsPage(
            @RequestParam(required = false) String token,
            Model model,
            HttpServletRequest request) {
        log.info("Accessing service requests page");

        // Get token from various sources
        String validToken = getValidToken(token, request);

        if (validToken == null) {
            log.warn("No valid token found, redirecting to login");
            return "redirect:/admin/login?error=session_expired";
        }

        // Set the admin's name for the page
        model.addAttribute("userName", "Arthur Morgan");

        return "admin/serviceRequests";
    }

    // Add this missing endpoint to handle GET requests for service requests list
    @GetMapping("/admin/service-requests/api")
    @ResponseBody
    public ResponseEntity<List<ServiceRequestDto>> getAllServiceRequests(
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Getting all service requests");
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<ServiceRequestDto> serviceRequests = serviceRequestService.getAllServiceRequests(validToken);
            log.info("Service requests loaded: {}", serviceRequests.size());
            return ResponseEntity.ok(serviceRequests);
        } catch (Exception e) {
            log.error("Error fetching service requests: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/admin/service-requests/api")
    @ResponseBody
    public ResponseEntity<?> createServiceRequest(
            @RequestBody ServiceRequestDto serviceRequestDto,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Creating service request: {}", serviceRequestDto);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceRequestDto createdRequest = serviceRequestService.createServiceRequest(serviceRequestDto, validToken);
            return ResponseEntity.ok(createdRequest);
        } catch (Exception e) {
            log.error("Error creating service request: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create service request: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/service-requests/api/{id}/assign")
    @ResponseBody
    public ResponseEntity<?> assignServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> requestBody,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Assigning service advisor to request ID {}: {}", id, requestBody);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Integer advisorId = requestBody.get("advisorId");
            if (advisorId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Advisor ID is required"));
            }

            ServiceRequestDto updatedRequest = serviceRequestService.assignServiceAdvisor(id, advisorId, validToken);
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            log.error("Error assigning service advisor: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to assign service advisor: " + e.getMessage()));
        }
    }

    @PutMapping("/admin/service-requests/api/{id}/status")
    @ResponseBody
    public ResponseEntity<?> updateServiceStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> requestBody,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Updating status for request ID {}: {}", id, requestBody);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            String status = requestBody.get("status");
            if (status == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
            }

            ServiceRequestDto updatedRequest = serviceRequestService.updateServiceRequestStatus(id, status, validToken);
            return ResponseEntity.ok(updatedRequest);
        } catch (Exception e) {
            log.error("Error updating service status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update service status: " + e.getMessage()));
        }
    }

    @GetMapping("/admin/service-requests/api/{id}")
    @ResponseBody
    public ResponseEntity<?> getServiceRequestById(
            @PathVariable Integer id,
            @RequestParam(required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("Getting service request details for ID: {}", id);
        String validToken = getValidToken(token, authHeader, request);

        if (validToken == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ServiceRequestDto serviceRequest = serviceRequestService.getServiceRequestById(id, validToken);
            if (serviceRequest == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(serviceRequest);
        } catch (Exception e) {
            log.error("Error fetching service request details: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch service request details: " + e.getMessage()));
        }
    }

    /**
     * Helper method to get valid token from various sources
     */
    private String getValidToken(String tokenParam, String authHeader, HttpServletRequest request) {
        // Check token parameter first
        if (tokenParam != null && !tokenParam.isEmpty()) {
            log.debug("Using token from parameter");
            return tokenParam;
        }

        // Check authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Using token from Authorization header");
            return authHeader.substring(7);
        }

        // Check session
        HttpSession session = request.getSession(false);
        if (session != null) {
            String token = (String) session.getAttribute("jwt-token");
            if (token != null && !token.isEmpty()) {
                log.debug("Using token from session");
                return token;
            }
        }

        return null;
    }

    private String getValidToken(String tokenParam, HttpServletRequest request) {
        return getValidToken(tokenParam, null, request);
    }
}