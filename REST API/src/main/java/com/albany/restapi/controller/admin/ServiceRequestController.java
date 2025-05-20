package com.albany.restapi.controller.admin;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.exception.ServiceRequestExceptions;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.admin.ServiceRequestService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestController.class);
    private final ServiceRequestService serviceRequestService;

    /**
     * Get all service requests
     */
    @GetMapping("/api")
    public ResponseEntity<List<ServiceRequestDTO>> getAllServiceRequests() {
        try {
            List<ServiceRequestDTO> requests = serviceRequestService.getAllServiceRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            logger.error("Error fetching service requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get service request by ID
     */
    @GetMapping("/api/{id}")
    public ResponseEntity<ServiceRequestDTO> getServiceRequestById(@PathVariable("id") Integer requestId) {
        try {
            ServiceRequestDTO request = serviceRequestService.getServiceRequestById(requestId);
            return ResponseEntity.ok(request);
        } catch (ServiceRequestExceptions.ServiceRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching service request with ID: " + requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new service request with improved error handling
     */
    @PostMapping("/api")
    public ResponseEntity<?> createServiceRequest(@RequestBody ServiceRequestDTO requestDTO) {
        try {
            logger.info("Creating service request: {}", requestDTO);
            
            // Basic validation
            if (requestDTO.getVehicleRegistration() == null 
                    && (requestDTO.getRegistrationNumber() != null)) {
                // Map registrationNumber to vehicleRegistration for compatibility
                requestDTO.setVehicleRegistration(requestDTO.getRegistrationNumber());
            }
            
            ServiceRequestDTO newRequest = serviceRequestService.createServiceRequest(requestDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(newRequest);
        } catch (EntityNotFoundException e) {
            logger.error("Entity not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (ServiceRequestExceptions.ValidationException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating service request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create service request: " + e.getMessage()));
        }
    }

    /**
     * Update an existing service request
     */
    @PutMapping("/api/{id}")
    public ResponseEntity<?> updateServiceRequest(
            @PathVariable("id") Integer requestId,
            @RequestBody ServiceRequestDTO requestDTO) {
        try {
            ServiceRequestDTO updatedRequest = serviceRequestService.updateServiceRequest(requestId, requestDTO);
            return ResponseEntity.ok(updatedRequest);
        } catch (ServiceRequestExceptions.ServiceRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating service request with ID: " + requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update service request: " + e.getMessage()));
        }
    }

    /**
     * Assign a service advisor to a service request
     */
    @PutMapping("/api/{id}/assign")
    public ResponseEntity<?> assignServiceAdvisor(
            @PathVariable("id") Integer requestId,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer advisorId = request.get("advisorId");
            if (advisorId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Service advisor ID is required"));
            }

            ServiceRequestDTO updatedRequest = serviceRequestService.assignServiceAdvisor(requestId, advisorId);
            return ResponseEntity.ok(updatedRequest);
        } catch (ServiceRequestExceptions.ServiceRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error assigning service advisor to request with ID: " + requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to assign service advisor: " + e.getMessage()));
        }
    }

    /**
     * Update the status of a service request
     */
    @PutMapping("/api/{id}/status")
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

            ServiceRequestDTO updatedRequest = serviceRequestService.updateServiceRequestStatus(requestId, status);
            return ResponseEntity.ok(updatedRequest);
        } catch (ServiceRequestExceptions.ServiceRequestNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating status for service request with ID: " + requestId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update service request status: " + e.getMessage()));
        }
    }
}