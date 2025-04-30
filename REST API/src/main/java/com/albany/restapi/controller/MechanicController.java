package com.albany.restapi.controller;

import com.albany.restapi.dto.MechanicRequest;
import com.albany.restapi.dto.MechanicResponse;
import com.albany.restapi.service.MechanicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mechanics")
@RequiredArgsConstructor
@Slf4j
public class MechanicController {

    private final MechanicService mechanicService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<MechanicResponse>> getAllMechanics() {
        log.info("API: Getting all mechanics");
        List<MechanicResponse> mechanics = mechanicService.getAllMechanics();
        return ResponseEntity.ok(mechanics);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<MechanicResponse> getMechanicById(@PathVariable Integer id) {
        log.info("API: Getting mechanic with ID: {}", id);
        MechanicResponse mechanic = mechanicService.getMechanicById(id);
        return ResponseEntity.ok(mechanic);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<MechanicResponse> createMechanic(@RequestBody MechanicRequest request) {
        log.info("API: Creating new mechanic: {}", request.getEmail());

        try {
            MechanicResponse newMechanic = mechanicService.createMechanic(request);
            log.info("Successfully created mechanic with ID: {}", newMechanic.getMechanicId());
            return ResponseEntity.ok(newMechanic);
        } catch (Exception e) {
            log.error("Error creating mechanic: {}", e.getMessage(), e);
            throw e; // Re-throw to let the exception handler deal with it
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<MechanicResponse> updateMechanic(
            @PathVariable Integer id,
            @RequestBody MechanicRequest request) {
        log.info("API: Updating mechanic with ID: {}", id);
        MechanicResponse updatedMechanic = mechanicService.updateMechanic(id, request);
        return ResponseEntity.ok(updatedMechanic);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Void> deleteMechanic(@PathVariable Integer id) {
        log.info("API: Deleting mechanic with ID: {}", id);
        mechanicService.deleteMechanic(id);
        return ResponseEntity.noContent().build();
    }

    // Additional endpoint to get mechanics by specialization
    @GetMapping("/specialization/{specialization}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<List<MechanicResponse>> getMechanicsBySpecialization(
            @PathVariable String specialization) {
        log.info("API: Getting mechanics with specialization: {}", specialization);
        List<MechanicResponse> mechanics = mechanicService.getAllMechanics()
                .stream()
                .filter(m -> m.getSpecialization().equalsIgnoreCase(specialization))
                .collect(Collectors.toList());
        return ResponseEntity.ok(mechanics);
    }

    // Endpoint to get counts of mechanics by specialization
    @GetMapping("/counts")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'SERVICE_ADVISOR', 'serviceAdvisor')")
    public ResponseEntity<Map<String, Long>> getMechanicCounts() {
        log.info("API: Getting mechanic counts by specialization");
        
        List<MechanicResponse> allMechanics = mechanicService.getAllMechanics();
        
        Map<String, Long> counts = allMechanics.stream()
                .collect(Collectors.groupingBy(
                        MechanicResponse::getSpecialization,
                        Collectors.counting()
                ));
        
        // Add total count
        counts.put("total", (long) allMechanics.size());
        
        return ResponseEntity.ok(counts);
    }
}