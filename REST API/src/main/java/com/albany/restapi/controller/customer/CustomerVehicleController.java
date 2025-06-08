package com.albany.restapi.controller.customer;

import com.albany.restapi.dto.VehicleDTO;
import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.User;
import com.albany.restapi.model.Vehicle;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer")
public class CustomerVehicleController {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerVehicleController.class);
    
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    
    public CustomerVehicleController(VehicleRepository vehicleRepository, 
                                     CustomerRepository customerRepository) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
    }
    
    /**
     * Get all vehicles for the authenticated customer
     */
    @GetMapping("/vehicles")
    public ResponseEntity<?> getCustomerVehicles(Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            logger.info("Fetching vehicles for user: {}", user.getEmail());
            
            // Get customer profile for this user
            CustomerProfile customer = customerRepository.findByUser_UserId(user.getUserId())
                .orElse(null);
            
            if (customer == null) {
                logger.warn("Customer profile not found for user: {}", user.getEmail());
                return ResponseEntity.ok(Collections.emptyList());
            }
            
            // Get all vehicles for this customer
            List<Vehicle> vehicles = vehicleRepository.findByCustomer_CustomerId(customer.getCustomerId());
            
            // Convert to DTOs
            List<VehicleDTO> vehicleDTOs = vehicles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            logger.info("Returning {} vehicles for user: {}", vehicleDTOs.size(), user.getEmail());
            return ResponseEntity.ok(vehicleDTOs);
        } catch (Exception e) {
            logger.error("Failed to load vehicles: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to load vehicles: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Add a new vehicle for the authenticated customer
     */
    @PostMapping("/vehicles/add")
    public ResponseEntity<?> addVehicle(@RequestBody VehicleDTO vehicleDTO, 
                                       Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            logger.info("Adding vehicle for user: {}", user.getEmail());
            
            // Get customer profile for this user
            CustomerProfile customer = customerRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found"));
            
            // Create new vehicle
            Vehicle vehicle = Vehicle.builder()
                .brand(vehicleDTO.getBrand())
                .model(vehicleDTO.getModel())
                .registrationNumber(vehicleDTO.getRegistrationNumber())
                .year(vehicleDTO.getYear())
                .category(Vehicle.Category.valueOf(vehicleDTO.getCategory()))
                .customer(customer)
                .build();
            
            // Save vehicle
            Vehicle savedVehicle = vehicleRepository.save(vehicle);
            logger.info("Vehicle added successfully with ID: {}", savedVehicle.getVehicleId());
            
            return ResponseEntity.ok(convertToDTO(savedVehicle));
        } catch (Exception e) {
            logger.error("Failed to add vehicle: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to add vehicle: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Update an existing vehicle
     */
    @PutMapping("/vehicles/update/{vehicleId}")
    public ResponseEntity<?> updateVehicle(@PathVariable Integer vehicleId,
                                         @RequestBody VehicleDTO vehicleDTO,
                                         Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            logger.info("Updating vehicle {} for user: {}", vehicleId, user.getEmail());
            
            // Get customer profile for this user
            CustomerProfile customer = customerRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found"));
            
            // Find the vehicle
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                logger.warn("Vehicle not found: {}", vehicleId);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Vehicle not found"
                ));
            }
            
            Vehicle vehicle = vehicleOpt.get();
            
            // Verify that the vehicle belongs to this customer
            if (!vehicle.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                logger.warn("Vehicle {} does not belong to user {}", vehicleId, user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Vehicle does not belong to this customer"
                ));
            }
            
            // Update vehicle
            vehicle.setBrand(vehicleDTO.getBrand());
            vehicle.setModel(vehicleDTO.getModel());
            vehicle.setRegistrationNumber(vehicleDTO.getRegistrationNumber());
            vehicle.setYear(vehicleDTO.getYear());
            vehicle.setCategory(Vehicle.Category.valueOf(vehicleDTO.getCategory()));
            
            // Save updated vehicle
            Vehicle updatedVehicle = vehicleRepository.save(vehicle);
            logger.info("Vehicle updated successfully: {}", vehicleId);
            
            return ResponseEntity.ok(convertToDTO(updatedVehicle));
        } catch (Exception e) {
            logger.error("Failed to update vehicle {}: {}", vehicleId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to update vehicle: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Delete a vehicle
     */
    @DeleteMapping("/vehicles/delete/{vehicleId}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Integer vehicleId,
                                         Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            logger.info("Deleting vehicle {} for user: {}", vehicleId, user.getEmail());
            
            // Get customer profile for this user
            CustomerProfile customer = customerRepository.findByUser_UserId(user.getUserId())
                .orElseThrow(() -> new IllegalStateException("Customer profile not found"));
            
            // Find the vehicle
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
            if (vehicleOpt.isEmpty()) {
                logger.warn("Vehicle not found: {}", vehicleId);
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Vehicle not found"
                ));
            }
            
            Vehicle vehicle = vehicleOpt.get();
            
            // Verify that the vehicle belongs to this customer
            if (!vehicle.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                logger.warn("Vehicle {} does not belong to user {}", vehicleId, user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Vehicle does not belong to this customer"
                ));
            }
            
            // Delete vehicle
            vehicleRepository.delete(vehicle);
            logger.info("Vehicle deleted successfully: {}", vehicleId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Vehicle deleted successfully"
            ));
        } catch (Exception e) {
            logger.error("Failed to delete vehicle {}: {}", vehicleId, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to delete vehicle: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Convert Vehicle entity to VehicleDTO
     */
    private VehicleDTO convertToDTO(Vehicle vehicle) {
        return VehicleDTO.builder()
            .vehicleId(vehicle.getVehicleId())
            .brand(vehicle.getBrand())
            .model(vehicle.getModel())
            .registrationNumber(vehicle.getRegistrationNumber())
            .year(vehicle.getYear())
            .category(vehicle.getCategory().name())
            .customerId(vehicle.getCustomer().getCustomerId())
            .customerName(vehicle.getCustomer().getUser().getFirstName() + " " + 
                         vehicle.getCustomer().getUser().getLastName())
            .build();
    }
}