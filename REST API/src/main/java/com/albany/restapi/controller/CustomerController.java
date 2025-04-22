package com.albany.restapi.controller;

import com.albany.restapi.dto.CustomerRequest;
import com.albany.restapi.exception.CustomerNotFoundException;
import com.albany.restapi.exception.CustomerValidationException;
import com.albany.restapi.exception.DuplicateEmailException;
import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.Role;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.CustomerProfileRepository;
import com.albany.restapi.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<List<Map<String, Object>>> getAllCustomers() {
        List<CustomerProfile> customers = customerProfileRepository.findAllActive();

        List<Map<String, Object>> response = customers.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Integer id) {
        return customerProfileRepository.findById(id)
                .map(this::convertToResponseDto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<?> createCustomer(@Valid @RequestBody CustomerRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("A user with this email already exists. Please use a different email address.");
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
            profile.setCustomerId(savedUser.getUserId()); // Set customerId to match userId
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
            Map<String, Object> response = convertToResponseDto(savedProfile);
            response.put("tempPassword", tempPassword); // Include the temp password in response

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            throw new CustomerValidationException("Error creating customer: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerRequest request) {

        try {
            CustomerProfile profile = customerProfileRepository.findById(id)
                    .orElseThrow(() -> new CustomerNotFoundException("Customer not found"));

            // Check if email is being changed to one that already exists
            String newEmail = request.getEmail();
            String currentEmail = profile.getUser().getEmail();

            if (!newEmail.equals(currentEmail) && userRepository.existsByEmail(newEmail)) {
                throw new DuplicateEmailException("A user with this email already exists. Please use a different email address.");
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

            return ResponseEntity.ok(convertToResponseDto(updatedProfile));
        } catch (Exception e) {
            throw new CustomerValidationException("Error updating customer: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        return customerProfileRepository.findById(id)
                .map(profile -> {
                    // Soft delete - mark as inactive
                    User user = profile.getUser();
                    user.setActive(false);
                    userRepository.save(user);

                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
    }

    private Map<String, Object> convertToResponseDto(CustomerProfile profile) {
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