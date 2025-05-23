package com.albany.restapi.controller.serviceAdvisor;

import com.albany.restapi.dto.LaborChargeDTO;
import com.albany.restapi.dto.MaterialRequestDTO;
import com.albany.restapi.dto.ServiceDetailDTO;
import com.albany.restapi.dto.ServiceStatusUpdateDTO;
import com.albany.restapi.service.serviceAdvisor.ServiceAdvisorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/serviceAdvisor/api")
@RequiredArgsConstructor
public class ServiceAdvisorDashboardController {

    private final ServiceAdvisorDashboardService dashboardService;

    /**
     * Get all vehicles assigned to the service advisor
     */
    @GetMapping("/assigned-vehicles")
    public ResponseEntity<List<Map<String, Object>>> getAssignedVehicles(
            @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(dashboardService.getAssignedVehicles(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get service details for a specific request
     */
    @GetMapping("/service-details/{requestId}")
    public ResponseEntity<ServiceDetailDTO> getServiceDetails(
            @PathVariable Integer requestId,
            @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(dashboardService.getServiceDetails(requestId, token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all available inventory items
     */
    @GetMapping("/inventory-items")
    public ResponseEntity<List<Map<String, Object>>> getInventoryItems(
            @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(dashboardService.getAvailableInventoryItems(token));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update service status
     */
    @PutMapping("/service/{requestId}/status")
    public ResponseEntity<Map<String, Object>> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody ServiceStatusUpdateDTO statusUpdate,
            @RequestHeader("Authorization") String token) {
        try {
            return ResponseEntity.ok(dashboardService.updateServiceStatus(requestId, statusUpdate, token));
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Add multiple labor charges - used by the "Save" button or bulk updates
     */
    @PostMapping("/service/{requestId}/labor-charges")
    public ResponseEntity<Map<String, Object>> addLaborCharges(
            @PathVariable Integer requestId,
            @RequestBody List<LaborChargeDTO> laborCharges,
            @RequestHeader("Authorization") String token) {
        try {
            // Validate input data
            if (laborCharges == null || laborCharges.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No labor charges provided");
                error.put("status", "error");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Check for at least one valid labor charge
            boolean hasValidCharge = false;
            for (LaborChargeDTO charge : laborCharges) {
                if (charge != null && charge.getHours() != null && charge.getRate() != null && 
                    charge.getHours().compareTo(BigDecimal.ZERO) > 0 && 
                    charge.getRate().compareTo(BigDecimal.ZERO) > 0) {
                    hasValidCharge = true;
                    break;
                }
            }
            
            if (!hasValidCharge) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No valid labor charges found. Hours and rate must be greater than zero.");
                error.put("status", "error");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Process labor charges
            Map<String, Object> result = dashboardService.updateLaborCharges(requestId, laborCharges, token);
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("exception_type", e.getClass().getName());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Add a single labor charge - used by the "Add" button for immediate database update
     */
    @PostMapping("/service/{requestId}/single-labor-charge")
    public ResponseEntity<Map<String, Object>> addSingleLaborCharge(
            @PathVariable Integer requestId,
            @RequestBody LaborChargeDTO laborCharge,
            @RequestHeader("Authorization") String token) {
        try {
            // Validate input data
            if (laborCharge == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No labor charge provided");
                error.put("status", "error");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Check if this is a valid labor charge
            if (laborCharge.getHours() == null || laborCharge.getRate() == null ||
                laborCharge.getHours().compareTo(BigDecimal.ZERO) <= 0 || 
                laborCharge.getRate().compareTo(BigDecimal.ZERO) <= 0) {
                
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Invalid labor charge. Hours and rate must be greater than zero.");
                error.put("status", "error");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Process single labor charge by wrapping in a list
            List<LaborChargeDTO> charges = Collections.singletonList(laborCharge);
            Map<String, Object> result = dashboardService.updateLaborCharges(requestId, charges, token);
            
            // Add success status
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("exception_type", e.getClass().getName());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get current labor charges
     */
    @GetMapping("/service/{requestId}/labor-charges")
    public ResponseEntity<Map<String, Object>> getLaborCharges(
            @PathVariable Integer requestId,
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> result = dashboardService.getLaborCharges(requestId, token);
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Add inventory items
     */
    @PostMapping("/service/{requestId}/inventory-items")
    public ResponseEntity<Map<String, Object>> addInventoryItems(
            @PathVariable Integer requestId,
            @RequestBody MaterialRequestDTO materialRequest,
            @RequestHeader("Authorization") String token) {
        try {
            if (materialRequest == null || materialRequest.getItems() == null || materialRequest.getItems().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No inventory items provided");
                error.put("status", "error");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> result = dashboardService.updateInventoryItems(requestId, materialRequest, token);
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Mark service as complete
     */
    @PutMapping("/service/{requestId}/complete")
    public ResponseEntity<Map<String, Object>> markServiceComplete(
            @PathVariable Integer requestId,
            @RequestHeader("Authorization") String token) {
        try {
            ServiceStatusUpdateDTO statusUpdate = new ServiceStatusUpdateDTO();
            statusUpdate.setStatus("Completed");
            statusUpdate.setNotes("Service completed by service advisor");
            
            Map<String, Object> result = dashboardService.updateServiceStatus(requestId, statusUpdate, token);
            result.put("status", "success");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}