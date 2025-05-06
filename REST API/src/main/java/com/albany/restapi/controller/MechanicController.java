package com.albany.restapi.controller;

import com.albany.restapi.dto.MechanicRequest;
import com.albany.restapi.dto.MechanicResponse;
import com.albany.restapi.service.MechanicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mechanics")
@RequiredArgsConstructor
@Slf4j
public class MechanicController {

    private final MechanicService mechanicService;

    /**
     * Get all mechanics
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<MechanicResponse>> getAllMechanics() {
        log.info("API: Getting all mechanics");
        List<MechanicResponse> mechanics = mechanicService.getAllMechanics();
        return ResponseEntity.ok(mechanics);
    }

    /**
     * Get mechanic by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<MechanicResponse> getMechanicById(@PathVariable Integer id) {
        log.info("API: Getting mechanic with ID: {}", id);
        try {
            MechanicResponse mechanic = mechanicService.getMechanicById(id);
            return ResponseEntity.ok(mechanic);
        } catch (Exception e) {
            log.error("Error getting mechanic with ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new mechanic
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<?> createMechanic(@RequestBody MechanicRequest request) {
        log.info("API: Creating new mechanic: {}", request.getEmail());

        try {
            // Validate request
            if (request.getEmail() == null || request.getFirstName() == null || request.getLastName() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email, first name, and last name are required"));
            }

            // Store the original password (if any) before sending to service
            String originalPassword = request.getPassword();

            // Create the mechanic - the service will generate a password if none is provided
            MechanicResponse newMechanic = mechanicService.createMechanic(request);

            // Create response with the generated password
            Map<String, Object> response = new HashMap<>();
            response.put("mechanic", newMechanic);

            // If password was generated, it will be in the request object
            if (request.getPassword() != null &&
                    (originalPassword == null || !originalPassword.equals(request.getPassword()))) {
                response.put("tempPassword", request.getPassword());
            }

            log.info("Successfully created mechanic with ID: {}", newMechanic.getMechanicId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating mechanic: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update an existing mechanic
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<?> updateMechanic(
            @PathVariable Integer id,
            @RequestBody MechanicRequest request) {
        log.info("API: Updating mechanic with ID: {}", id);

        try {
            MechanicResponse updatedMechanic = mechanicService.updateMechanic(id, request);
            return ResponseEntity.ok(updatedMechanic);
        } catch (Exception e) {
            log.error("Error updating mechanic: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete a mechanic (soft delete)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<?> deleteMechanic(@PathVariable Integer id) {
        log.info("API: Deleting mechanic with ID: {}", id);

        try {
            mechanicService.deleteMechanic(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting mechanic: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get mechanics by specialization
     */
    @GetMapping("/specialization/{specialization}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<MechanicResponse>> getMechanicsBySpecialization(
            @PathVariable String specialization) {
        log.info("API: Getting mechanics with specialization: {}", specialization);

        try {
            List<MechanicResponse> mechanics = mechanicService.getAllMechanics()
                    .stream()
                    .filter(m -> m.getSpecialization().equalsIgnoreCase(specialization))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(mechanics);
        } catch (Exception e) {
            log.error("Error getting mechanics by specialization: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get counts of mechanics by specialization
     */
    @GetMapping("/counts")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<Map<String, Long>> getMechanicCounts() {
        log.info("API: Getting mechanic counts by specialization");

        try {
            List<MechanicResponse> allMechanics = mechanicService.getAllMechanics();

            Map<String, Long> counts = allMechanics.stream()
                    .filter(m -> m.getSpecialization() != null && !m.getSpecialization().isEmpty())
                    .collect(Collectors.groupingBy(
                            MechanicResponse::getSpecialization,
                            Collectors.counting()
                    ));

            // Add total count
            counts.put("total", (long) allMechanics.size());

            // Add active count
            long activeCount = allMechanics.stream().filter(MechanicResponse::isActive).count();
            counts.put("active", activeCount);

            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            log.error("Error getting mechanic counts: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}