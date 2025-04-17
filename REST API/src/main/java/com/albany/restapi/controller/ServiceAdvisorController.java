package com.albany.restapi.controller;

import com.albany.restapi.dto.ServiceAdvisorRequest;
import com.albany.restapi.dto.ServiceAdvisorResponse;
import com.albany.restapi.service.ServiceAdvisorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-advisors")
@RequiredArgsConstructor
@Slf4j
public class ServiceAdvisorController {

    private final ServiceAdvisorService advisorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<ServiceAdvisorResponse>> getAllServiceAdvisors() {
        log.info("API: Getting all service advisors");
        List<ServiceAdvisorResponse> advisors = advisorService.getAllServiceAdvisors();
        return ResponseEntity.ok(advisors);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceAdvisorResponse> getServiceAdvisorById(@PathVariable Integer id) {
        log.info("API: Getting service advisor with ID: {}", id);
        ServiceAdvisorResponse advisor = advisorService.getServiceAdvisorById(id);
        return ResponseEntity.ok(advisor);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceAdvisorResponse> createServiceAdvisor(@RequestBody ServiceAdvisorRequest request) {
        log.info("API: Creating new service advisor: {}", request.getEmail());

        try {
            ServiceAdvisorResponse newAdvisor = advisorService.createServiceAdvisor(request);
            log.info("Successfully created service advisor with ID: {}", newAdvisor.getAdvisorId());
            return ResponseEntity.ok(newAdvisor);
        } catch (Exception e) {
            log.error("Error creating service advisor: {}", e.getMessage(), e);
            throw e; // Re-throw to let the exception handler deal with it
        }
    }

    // Alternative method to accept a Map instead of a strongly typed request
    @PostMapping("/create-from-map")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceAdvisorResponse> createServiceAdvisorFromMap(@RequestBody Map<String, Object> requestMap) {
        log.info("API: Creating new service advisor from map");

        try {
            // Convert map to request object
            ServiceAdvisorRequest request = new ServiceAdvisorRequest();

            if (requestMap.containsKey("firstName")) {
                request.setFirstName((String) requestMap.get("firstName"));
            }

            if (requestMap.containsKey("lastName")) {
                request.setLastName((String) requestMap.get("lastName"));
            }

            if (requestMap.containsKey("email")) {
                request.setEmail((String) requestMap.get("email"));
            }

            if (requestMap.containsKey("phoneNumber")) {
                request.setPhoneNumber((String) requestMap.get("phoneNumber"));
            }

            if (requestMap.containsKey("password")) {
                request.setPassword((String) requestMap.get("password"));
            }

            if (requestMap.containsKey("department")) {
                request.setDepartment((String) requestMap.get("department"));
            }

            if (requestMap.containsKey("specialization")) {
                request.setSpecialization((String) requestMap.get("specialization"));
            }

            ServiceAdvisorResponse newAdvisor = advisorService.createServiceAdvisor(request);
            log.info("Successfully created service advisor with ID: {}", newAdvisor.getAdvisorId());
            return ResponseEntity.ok(newAdvisor);
        } catch (Exception e) {
            log.error("Error creating service advisor from map: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceAdvisorResponse> updateServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody ServiceAdvisorRequest request) {
        log.info("API: Updating service advisor with ID: {}", id);
        ServiceAdvisorResponse updatedAdvisor = advisorService.updateServiceAdvisor(id, request);
        return ResponseEntity.ok(updatedAdvisor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Void> deleteServiceAdvisor(@PathVariable Integer id) {
        log.info("API: Deleting service advisor with ID: {}", id);
        advisorService.deleteServiceAdvisor(id);
        return ResponseEntity.noContent().build();
    }
}