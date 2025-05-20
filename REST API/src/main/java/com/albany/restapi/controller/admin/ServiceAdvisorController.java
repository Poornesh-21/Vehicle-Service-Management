package com.albany.restapi.controller.admin;

import com.albany.restapi.dto.ServiceAdvisorDTO;
import com.albany.restapi.service.admin.ServiceAdvisorService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/service-advisors")
@RequiredArgsConstructor
public class ServiceAdvisorController {

    private final ServiceAdvisorService serviceAdvisorService;

    /**
     * Get all service advisors
     */
    @GetMapping("/api/advisors")
    public ResponseEntity<List<ServiceAdvisorDTO>> getAllServiceAdvisors() {
        return ResponseEntity.ok(serviceAdvisorService.getAllServiceAdvisors());
    }

    /**
     * Get a service advisor by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceAdvisorDTO> getServiceAdvisorById(@PathVariable("id") Integer advisorId) {
        try {
            return ResponseEntity.ok(serviceAdvisorService.getServiceAdvisorById(advisorId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new service advisor
     * If password is not provided, a random one will be generated
     */
    @PostMapping
    public ResponseEntity<?> createServiceAdvisor(@RequestBody ServiceAdvisorDTO advisorDTO) {
        try {
            // If password is not provided, it will be generated in the service
            ServiceAdvisorDTO newAdvisor = serviceAdvisorService.createServiceAdvisor(advisorDTO);

            // Don't return the password in the response
            newAdvisor.setPassword(null);
            return new ResponseEntity<>(newAdvisor, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to create service advisor: " + e.getMessage()));
        }
    }

    /**
     * Update an existing service advisor
     * If password is provided, it will be updated and an email will be sent
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateServiceAdvisor(
            @PathVariable("id") Integer advisorId,
            @RequestBody ServiceAdvisorDTO advisorDTO) {
        try {
            // Set the ID to ensure we're updating the correct record
            advisorDTO.setAdvisorId(advisorId);

            ServiceAdvisorDTO updatedAdvisor = serviceAdvisorService.updateServiceAdvisor(advisorId, advisorDTO);

            // Don't return the password in the response
            updatedAdvisor.setPassword(null);
            return ResponseEntity.ok(updatedAdvisor);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update service advisor: " + e.getMessage()));
        }
    }
}