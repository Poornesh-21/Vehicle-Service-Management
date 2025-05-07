package com.albany.restapi.controller;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.ServiceAdvisorDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-advisor/dashboard")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorDashboardController {

    private final ServiceAdvisorDashboardService dashboardService;

    /**
     * Get all service requests assigned to the authenticated service advisor
     */
    @GetMapping("/assigned-vehicles")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<VehicleInServiceDTO>> getAssignedVehicles(Authentication authentication) {
        log.info("Fetching assigned vehicles for service advisor: {}", authentication.getName());
        List<VehicleInServiceDTO> assignedVehicles = dashboardService.getAssignedVehicles(authentication.getName());
        return ResponseEntity.ok(assignedVehicles);
    }

    /**
     * Get all new service requests that need to be assigned
     */
    @GetMapping("/new-assignments")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor', 'ADMIN', 'admin')")
    public ResponseEntity<List<ServiceRequestDTO>> getNewAssignments() {
        log.info("Fetching new service requests that need assignment");
        List<ServiceRequestDTO> newRequests = dashboardService.getNewServiceRequests();
        return ResponseEntity.ok(newRequests);
    }

    /**
     * Add inventory items to a service request
     */
    @PostMapping("/{requestId}/inventory-items")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceBillDTO> addInventoryItems(
            @PathVariable Integer requestId,
            @RequestBody MaterialItemListDTO itemsDto,
            Authentication authentication) {
        log.info("Adding inventory items to service request: {}", requestId);
        ServiceBillDTO result = dashboardService.addInventoryItems(requestId, itemsDto.getItems(), authentication.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * Add labor charges to a service request
     */
    @PostMapping("/{requestId}/labor-charges")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceBillDTO> addLaborCharges(
            @PathVariable Integer requestId,
            @RequestBody List<LaborChargeDTO> laborCharges,
            Authentication authentication) {
        log.info("Adding labor charges to service request: {}", requestId);
        ServiceBillDTO result = dashboardService.addLaborCharges(requestId, laborCharges, authentication.getName());
        return ResponseEntity.ok(result);
    }

    /**
     * Update service request status
     */
    @PutMapping("/{requestId}/status")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequestDTO> updateServiceStatus(
            @PathVariable Integer requestId,
            @RequestBody Map<String, String> statusUpdate,
            Authentication authentication) {
        log.info("Updating status for service request: {}", requestId);
        
        String status = statusUpdate.get("status");
        String notes = statusUpdate.get("notes");
        Boolean notifyCustomer = Boolean.valueOf(statusUpdate.getOrDefault("notifyCustomer", "false"));
        
        ServiceRequest.Status newStatus;
        try {
            newStatus = ServiceRequest.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status: {}", status);
            return ResponseEntity.badRequest().build();
        }
        
        ServiceRequestDTO updatedRequest = dashboardService.updateServiceStatus(
                requestId, 
                newStatus, 
                notes, 
                notifyCustomer,
                authentication.getName()
        );
        
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * Generate bill for a service request
     */
    @PostMapping("/{requestId}/generate-bill")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<BillResponseDTO> generateBill(
            @PathVariable Integer requestId,
            @RequestBody BillRequestDTO billRequest,
            Authentication authentication) {
        log.info("Generating bill for service request: {}", requestId);
        BillResponseDTO bill = dashboardService.generateBill(requestId, billRequest, authentication.getName());
        return ResponseEntity.ok(bill);
    }

    /**
     * Get service request details
     */
    @GetMapping("/{requestId}/details")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequestDetailDTO> getServiceRequestDetails(
            @PathVariable Integer requestId,
            Authentication authentication) {
        log.info("Fetching details for service request: {}", requestId);
        ServiceRequestDetailDTO details = dashboardService.getServiceRequestDetails(requestId, authentication.getName());
        return ResponseEntity.ok(details);
    }

    /**
     * Get current bill for a service request
     */
    @GetMapping("/{requestId}/current-bill")
    @PreAuthorize("hasAnyRole('SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceBillDTO> getCurrentBill(
            @PathVariable Integer requestId,
            Authentication authentication) {
        log.info("Fetching current bill for service request: {}", requestId);
        ServiceBillDTO bill = dashboardService.getCurrentBill(requestId, authentication.getName());
        return ResponseEntity.ok(bill);
    }
}