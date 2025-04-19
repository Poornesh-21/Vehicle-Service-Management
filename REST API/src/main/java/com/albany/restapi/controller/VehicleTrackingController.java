package com.albany.restapi.controller;

import com.albany.restapi.dto.CompletedServiceDTO;
import com.albany.restapi.dto.VehicleInServiceDTO;
import com.albany.restapi.model.Invoice;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.VehicleTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicle-tracking")
@RequiredArgsConstructor
@Slf4j
public class VehicleTrackingController {

    private final VehicleTrackingService vehicleTrackingService;

    /**
     * Get all vehicles under service
     */
    @GetMapping("/vehicles-under-service")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<VehicleInServiceDTO>> getVehiclesUnderService() {
        log.info("Fetching all vehicles under service");
        return ResponseEntity.ok(vehicleTrackingService.getVehiclesUnderService());
    }

    /**
     * Get all completed services
     */
    @GetMapping("/completed-services")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<CompletedServiceDTO>> getCompletedServices() {
        log.info("Fetching all completed services");
        return ResponseEntity.ok(vehicleTrackingService.getCompletedServices());
    }

    /**
     * Get a specific service request by ID
     */
    @GetMapping("/service-request/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequest> getServiceRequestById(@PathVariable Integer id) {
        log.info("Fetching service request with ID: {}", id);
        return vehicleTrackingService.getServiceRequestById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update the status of a service request
     */
    @PutMapping("/service-request/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequest> updateServiceStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> statusUpdate) {

        String statusStr = statusUpdate.get("status");
        if (statusStr == null || statusStr.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            ServiceRequest.Status newStatus = ServiceRequest.Status.valueOf(statusStr);
            log.info("Updating status of service request {} to {}", id, newStatus);
            ServiceRequest updatedRequest = vehicleTrackingService.updateServiceStatus(id, newStatus);
            return ResponseEntity.ok(updatedRequest);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", statusStr);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating service request status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Record a payment for a service
     */
    @PostMapping("/service-request/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> recordPayment(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> paymentDetails) {

        try {
            log.info("Recording payment for service request {}", id);
            vehicleTrackingService.recordPayment(id, paymentDetails);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Payment recorded successfully",
                    "requestId", id
            ));
        } catch (Exception e) {
            log.error("Error recording payment: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to record payment: " + e.getMessage()
            ));
        }
    }

    /**
     * Generate an invoice for a service
     */
    @PostMapping("/service-request/{id}/invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> generateInvoice(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> invoiceDetails) {

        try {
            log.info("Generating invoice for service request {}", id);
            Invoice invoice = vehicleTrackingService.generateInvoice(id, invoiceDetails);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Invoice generated successfully",
                    "invoiceId", invoice.getInvoiceId(),
                    "requestId", id,
                    "totalAmount", invoice.getTotalAmount(),
                    "taxes", invoice.getTaxes(),
                    "netAmount", invoice.getNetAmount()
            ));
        } catch (Exception e) {
            log.error("Error generating invoice: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to generate invoice: " + e.getMessage()
            ));
        }
    }

    /**
     * Dispatch a vehicle after service
     */
    @PostMapping("/service-request/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<?> dispatchVehicle(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> dispatchDetails) {

        try {
            log.info("Dispatching vehicle for service request {}", id);
            vehicleTrackingService.dispatchVehicle(id, dispatchDetails);
            return ResponseEntity.ok().body(Map.of(
                    "message", "Vehicle dispatched successfully",
                    "requestId", id
            ));
        } catch (Exception e) {
            log.error("Error dispatching vehicle: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Failed to dispatch vehicle: " + e.getMessage()
            ));
        }
    }

    /**
     * Filter vehicles under service
     */
    @PostMapping("/vehicles-under-service/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<VehicleInServiceDTO>> filterVehiclesUnderService(
            @RequestBody Map<String, Object> filterCriteria) {

        log.info("Filtering vehicles under service with criteria: {}", filterCriteria);
        List<VehicleInServiceDTO> filteredVehicles = vehicleTrackingService.filterVehiclesUnderService(filterCriteria);
        return ResponseEntity.ok(filteredVehicles);
    }

    /**
     * Filter completed services
     */
    @PostMapping("/completed-services/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<CompletedServiceDTO>> filterCompletedServices(
            @RequestBody Map<String, Object> filterCriteria) {

        log.info("Filtering completed services with criteria: {}", filterCriteria);
        List<CompletedServiceDTO> filteredServices = vehicleTrackingService.filterCompletedServices(filterCriteria);
        return ResponseEntity.ok(filteredServices);
    }
}