package com.albany.restapi.controller;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.Vehicle;
import com.albany.restapi.repository.CustomerProfileRepository;
import com.albany.restapi.repository.VehicleRepository;
import com.albany.restapi.service.ServiceRequestService;
import com.albany.restapi.model.ServiceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final VehicleRepository vehicleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final VehicleController vehicleController;
    private final CustomerController customerController;
    private final ServiceRequestService serviceRequestService;

    /**
     * Get all customers
     */
    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> getAllCustomers() {
        log.info("Admin API: Getting all customers");
        return customerController.getAllCustomers();
    }

    /**
     * Get customer by ID
     */
    @GetMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> getCustomerById(@PathVariable Integer id) {
        log.info("Admin API: Getting customer with ID: {}", id);
        return customerController.getCustomerById(id);
    }

    /**
     * Create a new customer
     */
    @PostMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> createCustomer(@RequestBody Map<String, Object> request) {
        log.info("Admin API: Creating new customer");
        return customerController.createCustomer(request);
    }

    /**
     * Update a customer
     */
    @PutMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {
        log.info("Admin API: Updating customer with ID: {}", id);
        return customerController.updateCustomer(id, request);
    }

    /**
     * Delete a customer
     */
    @DeleteMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> deleteCustomer(@PathVariable Integer id) {
        log.info("Admin API: Deleting customer with ID: {}", id);
        return customerController.deleteCustomer(id);
    }

    /**
     * Get all vehicles for a specific customer - admin version
     */
    // Methods related to vehicles in AdminController.java

    /**
     * Get all vehicles for a specific customer - admin version
     */
    @GetMapping("/customers/{customerId}/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> getVehiclesForCustomer(@PathVariable Integer customerId) {
        log.info("Admin API: Getting vehicles for customer ID: {}", customerId);

        try {
            List<Vehicle> vehicles = vehicleRepository.findByCustomer_CustomerId(customerId);
            return ResponseEntity.ok(vehicles);
        } catch (Exception e) {
            log.error("Error getting vehicles for customer: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to get vehicles: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Create a new vehicle for a customer - admin version
     */
    @PostMapping("/customers/{customerId}/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    @Transactional
    public ResponseEntity<?> createVehicleForCustomer(
            @PathVariable Integer customerId,
            @RequestBody Map<String, Object> vehicleData) {

        log.info("Admin API: Creating vehicle for customer ID: {}", customerId);

        // Add the customerId to the vehicle data
        vehicleData.put("customerId", customerId);

        // Delegate to the vehicle controller's createVehicle method
        return vehicleController.createVehicle(vehicleData);
    }

    /**
     * Get vehicle by ID - admin version
     */
    @GetMapping("/vehicles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> getVehicleById(@PathVariable Integer id) {
        log.info("Admin API: Getting vehicle with ID: {}", id);
        return vehicleController.getVehicleById(id);
    }

    /**
     * Update a vehicle - admin version
     */
    @PutMapping("/vehicles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> updateVehicle(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> vehicleData) {

        log.info("Admin API: Updating vehicle with ID: {}", id);
        return vehicleController.updateVehicle(id, vehicleData);
    }

    /**
     * Delete a vehicle - admin version
     */
    @DeleteMapping("/vehicles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> deleteVehicle(@PathVariable Integer id) {
        log.info("Admin API: Deleting vehicle with ID: {}", id);
        return vehicleController.deleteVehicle(id);
    }

    /**
     * Get all service requests - admin version
     */
    @GetMapping("/service-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<ServiceRequestDTO>> getAllServiceRequests() {
        log.info("Admin API: Getting all service requests");
        List<ServiceRequestDTO> serviceRequests = serviceRequestService.getAllServiceRequests();
        return ResponseEntity.ok(serviceRequests);
    }

    /**
     * Get service request by ID - admin version
     */
    @GetMapping("/service-requests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> getServiceRequestById(@PathVariable Integer id) {
        log.info("Admin API: Getting service request with ID: {}", id);
        ServiceRequestDTO serviceRequest = serviceRequestService.getServiceRequestById(id);
        return ResponseEntity.ok(serviceRequest);
    }

    /**
     * Create a new service request - admin version
     */
    @PostMapping("/service-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> createServiceRequest(@RequestBody ServiceRequestDTO requestDTO) {
        log.info("Admin API: Creating new service request");
        ServiceRequestDTO createdRequest = serviceRequestService.createServiceRequest(requestDTO);
        return ResponseEntity.ok(createdRequest);
    }

    /**
     * Assign service advisor to a request - admin version
     */
    @PutMapping("/service-requests/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> assignServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> request) {
        
        log.info("Admin API: Assigning service advisor to request ID: {}", id);
        Integer advisorId = request.get("advisorId");
        ServiceRequestDTO updatedRequest = serviceRequestService.assignServiceAdvisor(id, advisorId);
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * Update service request status - admin version
     */
    @PutMapping("/service-requests/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> updateServiceRequestStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        
        log.info("Admin API: Updating status for service request ID: {}", id);
        try {
            ServiceRequest.Status status = ServiceRequest.Status.valueOf(request.get("status"));
            ServiceRequestDTO updatedRequest = serviceRequestService.updateServiceRequestStatus(id, status);
            return ResponseEntity.ok(updatedRequest);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", request.get("status"));
            throw new RuntimeException("Invalid status value: " + request.get("status"));
        }
    }
}