package com.albany.restapi.controller;

import com.albany.restapi.dto.MechanicAssignmentDTO;
import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.dto.VehicleInServiceDTO;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.MechanicAssignmentService;
import com.albany.restapi.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-assignments")
@RequiredArgsConstructor
@Slf4j
public class VehicleAssignmentController {

    private final MechanicAssignmentService assignmentService;
    private final ServiceRequestService serviceRequestService;

    /**
     * Get new service requests that need assignment
     */
    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<ServiceRequestDTO>> getNewServiceRequests(Authentication authentication) {
        log.info("Fetching new service requests for assignment");
        List<ServiceRequestDTO> newRequests = assignmentService.getNewServiceRequests();
        return ResponseEntity.ok(newRequests);
    }

    /**
     * Assign mechanics to a service request
     */
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequestDTO> assignMechanicsToServiceRequest(
            @RequestBody MechanicAssignmentDTO assignmentDTO,
            Authentication authentication) {
        
        log.info("Assigning mechanics to service request ID: {}", assignmentDTO.getServiceRequestId());
        
        ServiceRequestDTO updatedRequest = assignmentService.assignMechanicsToServiceRequest(
                assignmentDTO, 
                authentication.getName()
        );
        
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * Get all service requests that have been assigned to mechanics
     */
    @GetMapping("/assigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<VehicleInServiceDTO>> getAssignedRequests(Authentication authentication) {
        log.info("Fetching assigned service requests");
        List<VehicleInServiceDTO> assignedRequests = assignmentService.getAssignedRequests();
        return ResponseEntity.ok(assignedRequests);
    }
    
    /**
     * Update the status of a service request
     */
    @PutMapping("/{requestId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequestDTO> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> statusUpdate) {
        
        String statusStr = statusUpdate.get("status");
        ServiceRequest.Status status = ServiceRequest.Status.valueOf(statusStr);
        
        log.info("Updating service request {} to status: {}", requestId, status);
        ServiceRequestDTO updatedRequest = serviceRequestService.updateServiceRequestStatus(requestId, status);
        
        return ResponseEntity.ok(updatedRequest);
    }
}