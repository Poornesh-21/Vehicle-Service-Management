package com.albany.restapi.controller;

import com.albany.restapi.dto.CustomerRequest;
import com.albany.restapi.dto.ServiceRequestDTO;
import com.albany.restapi.model.*;
import com.albany.restapi.repository.*;
import com.albany.restapi.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/api")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestService serviceRequestService;
    private final PasswordEncoder passwordEncoder;

    // Customer Management Endpoints

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers() {
        List<CustomerProfile> customers = customerProfileRepository.findAllActive();

        List<Map<String, Object>> response = customers.stream()
                .map(this::convertToCustomerResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    @Transactional
    public ResponseEntity<?> createCustomer(@Valid @RequestBody CustomerRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "A user with this email already exists. Please use a different email address."));
        }

        try {
            // Create User entity
            User user = new User();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(request.getEmail());
            user.setPhoneNumber(request.getPhoneNumber());
            user.setRole(Role.customer);
            user.setActive(true);

            // Generate a temporary password
            String tempPassword = "Customer" + System.currentTimeMillis() % 10000;
            user.setPassword(passwordEncoder.encode(tempPassword));

            // Save the user
            User savedUser = userRepository.save(user);

            // Create CustomerProfile
            CustomerProfile profile = new CustomerProfile();
            profile.setCustomerId(savedUser.getUserId());
            profile.setUser(savedUser);
            profile.setStreet(request.getStreet());
            profile.setCity(request.getCity());
            profile.setState(request.getState());
            profile.setPostalCode(request.getPostalCode());
            profile.setMembershipStatus(request.getMembershipStatus());
            profile.setTotalServices(0);

            // Save the profile
            CustomerProfile savedProfile = customerProfileRepository.save(profile);

            // Return the created customer
            Map<String, Object> response = convertToCustomerResponseDto(savedProfile);
            response.put("tempPassword", tempPassword);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error creating customer", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Integer id) {
        return customerProfileRepository.findById(id)
                .map(this::convertToCustomerResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerRequest request) {
        try {
            CustomerProfile profile = customerProfileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            // Check if email is being changed to one that already exists
            String newEmail = request.getEmail();
            String currentEmail = profile.getUser().getEmail();

            if (!newEmail.equals(currentEmail) && userRepository.existsByEmail(newEmail)) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "A user with this email already exists. Please use a different email address."));
            }

            // Update User
            User user = profile.getUser();
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setEmail(newEmail);
            user.setPhoneNumber(request.getPhoneNumber());

            // Update CustomerProfile
            profile.setStreet(request.getStreet());
            profile.setCity(request.getCity());
            profile.setState(request.getState());
            profile.setPostalCode(request.getPostalCode());
            profile.setMembershipStatus(request.getMembershipStatus());

            // Save updates
            userRepository.save(user);
            CustomerProfile updatedProfile = customerProfileRepository.save(profile);

            return ResponseEntity.ok(convertToCustomerResponseDto(updatedProfile));
        } catch (Exception e) {
            log.error("Error updating customer", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/customers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    @Transactional
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        return customerProfileRepository.findById(id)
                .map(profile -> {
                    // Soft delete - mark as inactive
                    User user = profile.getUser();
                    user.setActive(false);
                    userRepository.save(user);

                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Vehicle Management Endpoints

    @GetMapping("/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<Vehicle>> getAllVehicles() {
        return ResponseEntity.ok(vehicleRepository.findAll());
    }

    @GetMapping("/customers/{customerId}/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<Vehicle>> getVehiclesForCustomer(@PathVariable Integer customerId) {
        return ResponseEntity.ok(vehicleRepository.findByCustomer_CustomerId(customerId));
    }

    // Service Request Management Endpoints

    @GetMapping("/service-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<ServiceRequestDTO>> getAllServiceRequests() {
        return ResponseEntity.ok(serviceRequestService.getAllServiceRequests());
    }

    @GetMapping("/service-requests/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<ServiceRequestDTO>> getServiceRequestsByStatus(@PathVariable ServiceRequest.Status status) {
        return ResponseEntity.ok(serviceRequestService.getServiceRequestsByStatus(status));
    }

    @PostMapping("/service-requests")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> createServiceRequest(@RequestBody ServiceRequestDTO requestDTO) {
        return ResponseEntity.ok(serviceRequestService.createServiceRequest(requestDTO));
    }

    @PutMapping("/service-requests/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<ServiceRequestDTO> assignServiceAdvisor(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> request) {
        Integer advisorId = request.get("advisorId");
        return ResponseEntity.ok(serviceRequestService.assignServiceAdvisor(id, advisorId));
    }

    // Helper Methods

    private Map<String, Object> convertToCustomerResponseDto(CustomerProfile profile) {
        Map<String, Object> dto = new HashMap<>();

        User user = profile.getUser();

        dto.put("customerId", profile.getCustomerId());
        dto.put("userId", user.getUserId());
        dto.put("firstName", user.getFirstName());
        dto.put("lastName", user.getLastName());
        dto.put("email", user.getEmail());
        dto.put("phoneNumber", user.getPhoneNumber());
        dto.put("street", profile.getStreet());
        dto.put("city", profile.getCity());
        dto.put("state", profile.getState());
        dto.put("postalCode", profile.getPostalCode());
        dto.put("totalServices", profile.getTotalServices());
        dto.put("lastServiceDate", profile.getLastServiceDate());
        dto.put("membershipStatus", profile.getMembershipStatus());
        dto.put("isActive", user.isActive());

        return dto;
    }
}