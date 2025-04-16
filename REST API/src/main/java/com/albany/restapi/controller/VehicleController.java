package com.albany.restapi.controller;

import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.Role;
import com.albany.restapi.model.User;
import com.albany.restapi.model.Vehicle;
import com.albany.restapi.repository.CustomerProfileRepository;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.repository.VehicleRepository;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final UserRepository userRepository;

    /**
     * Get all vehicles
     */
    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleRepository.findAll());
    }

    /**
     * Get all vehicles for a specific customer
     */
    @GetMapping("/customers/{customerId}/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<List<Vehicle>> getVehiclesForCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.ok(vehicleRepository.findByCustomer_CustomerId(customerId));
    }

    /**
     * Create a new vehicle for a specific customer
     */
    @PostMapping("/customers/{customerId}/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    @Transactional
    public ResponseEntity<?> createVehicleForCustomer(
            @PathVariable Integer customerId,
            @RequestBody Map<String, Object> vehicleData) {

        // Add the customerId to the vehicle data
        vehicleData.put("customerId", customerId);

        // Delegate to the existing createVehicle method
        return createVehicle(vehicleData);
    }

    /**
     * Get vehicle by ID
     */
    @GetMapping("/vehicles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Integer id) {
        return vehicleRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new vehicle
     */
    @PostMapping("/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    @Transactional
    public ResponseEntity<?> createVehicle(@RequestBody Map<String, Object> vehicleData) {
        try {
            log.debug("Received vehicle creation request with data: {}", vehicleData);

            // Extract customer ID
            Integer customerId = null;
            if (vehicleData.containsKey("customerId")) {
                customerId = Integer.valueOf(vehicleData.get("customerId").toString());
            }

            if (customerId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Customer ID is required"));
            }

            log.debug("Attempting to create vehicle for customer ID: {}", customerId);

            // Find the customer profile directly
            Optional<CustomerProfile> existingProfile = customerProfileRepository.findById(customerId);
            CustomerProfile customer;

            if (existingProfile.isPresent()) {
                // Use the existing profile
                customer = existingProfile.get();
                log.debug("Found existing CustomerProfile with ID: {}", customer.getCustomerId());
            } else {
                // Check if there's a User with this ID but no CustomerProfile yet
                Optional<User> userOpt = userRepository.findById(customerId);
                if (!userOpt.isPresent()) {
                    log.warn("No User found with ID: {}", customerId);
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "No user found with ID: " + customerId));
                }

                User user = userOpt.get();
                log.debug("Found User with ID: {}, role: {}", customerId, user.getRole());

                // Create a new CustomerProfile - we must save this FIRST before creating vehicle
                customer = new CustomerProfile();
                customer.setCustomerId(customerId); // Explicitly set ID to match user ID
                customer.setUser(user);
                customer.setMembershipStatus("Standard");
                customer.setTotalServices(0);

                // Save the customer profile first
                customer = customerProfileRepository.save(customer);
                log.debug("Created new CustomerProfile with ID: {}", customer.getCustomerId());
            }

            // Check for duplicate registration number
            if (vehicleData.containsKey("registrationNumber")) {
                String registrationNumber = vehicleData.get("registrationNumber").toString();
                if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
                    log.warn("Vehicle with registration number {} already exists", registrationNumber);
                    return ResponseEntity.badRequest().body(Map.of("error",
                            "Vehicle with this registration number already exists"));
                }
            }

            // Create new vehicle
            Vehicle vehicle = new Vehicle();
            vehicle.setCustomer(customer); // Set customer object, not just ID

            // Set vehicle properties from request data
            if (vehicleData.containsKey("brand")) {
                vehicle.setBrand(vehicleData.get("brand").toString());
            }

            if (vehicleData.containsKey("model")) {
                vehicle.setModel(vehicleData.get("model").toString());
            }

            if (vehicleData.containsKey("registrationNumber")) {
                vehicle.setRegistrationNumber(vehicleData.get("registrationNumber").toString());
            }

            if (vehicleData.containsKey("year")) {
                vehicle.setYear(Integer.valueOf(vehicleData.get("year").toString()));
            }

            if (vehicleData.containsKey("category")) {
                String categoryStr = vehicleData.get("category").toString();
                try {
                    Vehicle.Category category = Vehicle.Category.valueOf(categoryStr);
                    vehicle.setCategory(category);
                    log.debug("Set vehicle category to: {}", category);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid vehicle category: {}", categoryStr);
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid vehicle category"));
                }
            }

            // Save the vehicle
            log.debug("Saving vehicle with customer ID: {}", customer.getCustomerId());
            Vehicle savedVehicle = vehicleRepository.save(vehicle);
            log.debug("Successfully saved vehicle with ID: {}", savedVehicle.getVehicleId());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedVehicle);

        } catch (Exception e) {
            log.error("Error creating vehicle: {}", e.getMessage(), e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to create vehicle: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Update a vehicle
     */
    @PutMapping("/vehicles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin', 'CUSTOMER', 'customer')")
    public ResponseEntity<?> updateVehicle(@PathVariable Integer id, @RequestBody Map<String, Object> vehicleData) {
        try {
            // Find the vehicle
            Optional<Vehicle> vehicleOpt = vehicleRepository.findById(id);
            if (vehicleOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Vehicle vehicle = vehicleOpt.get();

            // Check for duplicate registration number
            if (vehicleData.containsKey("registrationNumber")) {
                String newRegNumber = vehicleData.get("registrationNumber").toString();
                String currentRegNumber = vehicle.getRegistrationNumber();

                if (!newRegNumber.equals(currentRegNumber) &&
                        vehicleRepository.existsByRegistrationNumber(newRegNumber)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Vehicle with this registration number already exists"));
                }

                vehicle.setRegistrationNumber(newRegNumber);
            }

            // Update vehicle properties
            if (vehicleData.containsKey("brand")) {
                vehicle.setBrand(vehicleData.get("brand").toString());
            }

            if (vehicleData.containsKey("model")) {
                vehicle.setModel(vehicleData.get("model").toString());
            }

            if (vehicleData.containsKey("year")) {
                vehicle.setYear(Integer.valueOf(vehicleData.get("year").toString()));
            }

            if (vehicleData.containsKey("category")) {
                String categoryStr = vehicleData.get("category").toString();
                try {
                    Vehicle.Category category = Vehicle.Category.valueOf(categoryStr);
                    vehicle.setCategory(category);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid vehicle category"));
                }
            }

            // Save the updated vehicle
            Vehicle savedVehicle = vehicleRepository.save(vehicle);

            return ResponseEntity.ok(savedVehicle);

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to update vehicle: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Delete a vehicle
     */
    @DeleteMapping("/vehicles/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> deleteVehicle(@PathVariable Integer id) {
        try {
            if (!vehicleRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            vehicleRepository.deleteById(id);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to delete vehicle: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}