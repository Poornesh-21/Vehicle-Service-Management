package com.albany.restapi.controller.admin;

import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.Vehicle;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.VehicleRepository;
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
@RequestMapping("/admin/api")
public class VehicleController {
    
    private static final Logger logger = LoggerFactory.getLogger(VehicleController.class);
    
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;

    public VehicleController(VehicleRepository vehicleRepository, 
                           CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Get all vehicles for a customer
     */
    @GetMapping("/customers/{customerId}/vehicles")
    public ResponseEntity<List<Vehicle>> getVehiclesForCustomer(@PathVariable Integer customerId) {
        logger.info("REST API: Getting vehicles for customer ID: {}", customerId);
        List<Vehicle> vehicles = vehicleRepository.findByCustomer_CustomerId(customerId);
        logger.info("Found {} vehicles for customer ID: {}", vehicles.size(), customerId);
        return ResponseEntity.ok(vehicles);
    }

    /**
     * Get vehicle by ID
     */
    @GetMapping("/vehicles/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Integer id) {
        logger.info("REST API: Getting vehicle with ID: {}", id);
        return vehicleRepository.findById(id)
                .map(vehicle -> {
                    logger.info("Found vehicle: {}", vehicle);
                    return ResponseEntity.ok(vehicle);
                })
                .orElseGet(() -> {
                    logger.warn("Vehicle not found with ID: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Create a new vehicle for a customer
     */
    @PostMapping("/customers/{customerId}/vehicles")
    public ResponseEntity<?> createVehicle(
            @PathVariable Integer customerId,
            @RequestBody Vehicle vehicle) {
        try {
            logger.info("REST API: Creating vehicle for customer ID: {}", customerId);
            logger.info("Vehicle data: {}", vehicle);
            
            CustomerProfile customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
            
            vehicle.setCustomer(customer);
            Vehicle savedVehicle = vehicleRepository.save(vehicle);
            
            logger.info("Vehicle created successfully with ID: {}", savedVehicle.getVehicleId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedVehicle);
        } catch (EntityNotFoundException e) {
            logger.error("Customer not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating vehicle: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create vehicle: " + e.getMessage()));
        }
    }
}