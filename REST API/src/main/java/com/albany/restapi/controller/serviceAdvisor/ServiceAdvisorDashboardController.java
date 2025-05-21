package com.albany.restapi.controller.serviceAdvisor;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.security.JwtUtil;
import com.albany.restapi.service.serviceAdvisor.ServiceAdvisorAuthService;
import com.albany.restapi.service.serviceAdvisor.ServiceAdvisorDashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/serviceAdvisor/api/dashboard")
@RequiredArgsConstructor
public class ServiceAdvisorDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceAdvisorDashboardController.class);
    
    private final ServiceAdvisorDashboardService dashboardService;
    private final ServiceAdvisorAuthService authService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @GetMapping("/service-requests")
    public ResponseEntity<?> getServiceRequests(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String status) {
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            
            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            
            // Validate the user is a service advisor
            if (user.getRole() != User.Role.serviceAdvisor) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Access denied. Not a service advisor."));
            }
            
            List<ServiceRequestDTO> serviceRequests;
            
            if (status != null && !status.isEmpty()) {
                // Try to parse the status
                try {
                    ServiceRequest.Status statusEnum = ServiceRequest.Status.valueOf(status);
                    serviceRequests = dashboardService.getServiceRequestsByStatus(username, statusEnum);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid status value. Valid values are: Received, Diagnosis, Repair, Completed"));
                }
            } else {
                serviceRequests = dashboardService.getAllServiceRequests(username);
            }
            
            return ResponseEntity.ok(serviceRequests);
            
        } catch (UsernameNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
        } catch (Exception e) {
            logger.error("Error fetching service requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch service requests: " + e.getMessage()));
        }
    }
    
    @GetMapping("/service-requests/{id}")
    public ResponseEntity<?> getServiceRequestDetails(
            @PathVariable("id") Integer requestId,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            
            ServiceRequestDTO serviceRequest = dashboardService.getServiceRequestDetails(username, requestId);
            return ResponseEntity.ok(serviceRequest);
            
        } catch (UsernameNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching service request details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch service request details: " + e.getMessage()));
        }
    }
    
    @PutMapping("/service-requests/{id}/status")
    public ResponseEntity<?> updateServiceRequestStatus(
            @PathVariable("id") Integer requestId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            
            String statusStr = request.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Status is required"));
            }
            
            ServiceRequest.Status status;
            try {
                status = ServiceRequest.Status.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid status value. Valid values are: Received, Diagnosis, Repair, Completed"));
            }
            
            ServiceRequestDTO updatedRequest = dashboardService.updateServiceRequestStatus(username, requestId, status);
            return ResponseEntity.ok(updatedRequest);
            
        } catch (UsernameNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating service request status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update service request status: " + e.getMessage()));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            
            Map<String, Object> stats = dashboardService.getDashboardStats(username);
            return ResponseEntity.ok(stats);
            
        } catch (UsernameNotFoundException e) {
            logger.warn("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
        } catch (Exception e) {
            logger.error("Error fetching dashboard stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch dashboard stats: " + e.getMessage()));
        }
    }
}