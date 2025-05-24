package com.albany.restapi.controller.admin;

import com.albany.restapi.service.admin.DashboardService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;

    @GetMapping("/api/data")
    public ResponseEntity<Map<String, Object>> getDashboardData() {
        try {
            logger.info("Fetching dashboard data");
            Map<String, Object> dashboardData = dashboardService.getDashboardData();
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            logger.error("Error fetching dashboard data: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Assign a service advisor to a service request
     * @param requestId The service request ID
     * @param advisorId The service advisor ID
     */
    @PutMapping("/api/assign/{requestId}")
    public ResponseEntity<?> assignAdvisor(
            @PathVariable Integer requestId,
            @RequestParam Integer advisorId) {
        
        try {
            logger.info("Assigning advisor ID: {} to request ID: {}", advisorId, requestId);
            boolean success = dashboardService.assignServiceAdvisorToRequest(requestId, advisorId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true, 
                    "message", "Service advisor assigned successfully",
                    "requestId", requestId,
                    "advisorId", advisorId
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Failed to assign service advisor"
                ));
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid input for advisor assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error assigning advisor: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "An error occurred while assigning the service advisor"
            ));
        }
    }
}