package com.albany.restapi.controller.admin;

import com.albany.restapi.dto.VehicleTrackingDTO;
import com.albany.restapi.exception.ServiceRequestExceptions;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.admin.VehicleTrackingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/api/vehicle-tracking")
@RequiredArgsConstructor
public class VehicleTrackingController {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleTrackingController.class);
    private final VehicleTrackingService vehicleTrackingService;
    
    /**
     * Get all vehicles currently under service (not completed)
     */
    @GetMapping("/under-service")
    public ResponseEntity<List<VehicleTrackingDTO>> getVehiclesUnderService() {
        try {
            List<VehicleTrackingDTO> vehicles = vehicleTrackingService.getVehiclesUnderService();
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            logger.error("Error fetching vehicles under service", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get details for a specific service request
     */
    @GetMapping("/service-request/{id}")
    public ResponseEntity<?> getServiceRequestDetails(@PathVariable("id") Integer requestId) {
        try {
            VehicleTrackingDTO serviceDetails = vehicleTrackingService.getServiceRequestDetails(requestId);
            return ResponseEntity.ok(serviceDetails);
        } catch (ServiceRequestExceptions.ServiceRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching service request details for ID: " + requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch service request details: " + e.getMessage()));
        }
    }
    
    /**
     * Update the status of a service request
     */
    @PutMapping("/service-request/{id}/status")
    public ResponseEntity<?> updateServiceRequestStatus(
            @PathVariable("id") Integer requestId,
            @RequestBody Map<String, String> request) {
        try {
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
            
            VehicleTrackingDTO updatedRequest = vehicleTrackingService.updateServiceRequestStatus(requestId, status);
            return ResponseEntity.ok(updatedRequest);
        } catch (ServiceRequestExceptions.ServiceRequestNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating status for service request with ID: " + requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update service request status: " + e.getMessage()));
        }
    }
    
    /**
     * Filter vehicles under service based on various criteria
     */
    @PostMapping("/under-service/filter")
    public ResponseEntity<List<VehicleTrackingDTO>> filterVehiclesUnderService(
            @RequestBody Map<String, Object> filterCriteria) {
        try {
            List<VehicleTrackingDTO> filteredVehicles = vehicleTrackingService.filterVehiclesUnderService(filterCriteria);
            return ResponseEntity.ok(filteredVehicles);
        } catch (Exception e) {
            logger.error("Error filtering vehicles under service", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}