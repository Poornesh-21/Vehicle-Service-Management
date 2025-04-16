package com.albany.restapi.controller;

import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.ServiceRequest;
import com.albany.restapi.service.ServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {
    
    private final ServiceRequestService serviceRequestService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<ServiceRequestDTO>> getAllServiceRequests() {
        return ResponseEntity.ok(serviceRequestService.getAllServiceRequests());
    }
    
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.ok(serviceRequestService.getServiceRequestsByCustomer(customerId));
    }
    
    @GetMapping("/advisor/{advisorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByAdvisor(@PathVariable Integer advisorId) {
        return ResponseEntity.ok(serviceRequestService.getServiceRequestsByAdvisor(advisorId));
    }
    
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByStatus(@PathVariable String status) {
        ServiceRequest.Status requestStatus = ServiceRequest.Status.valueOf(status);
        return ResponseEntity.ok(serviceRequestService.getServiceRequestsByStatus(requestStatus));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequestDTO> getServiceRequestById(@PathVariable Integer id) {
        return ResponseEntity.ok(serviceRequestService.getServiceRequestById(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<ServiceRequestDTO> createServiceRequest(@RequestBody ServiceRequestDTO requestDTO) {
        return ResponseEntity.ok(serviceRequestService.createServiceRequest(requestDTO));
    }
    
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> assignServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> request) {
        Integer advisorId = request.get("advisorId");
        return ResponseEntity.ok(serviceRequestService.assignServiceAdvisor(id, advisorId));
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<ServiceRequestDTO> updateServiceRequestStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        ServiceRequest.Status status = ServiceRequest.Status.valueOf(request.get("status"));
        return ResponseEntity.ok(serviceRequestService.updateServiceRequestStatus(id, status));
    }
}