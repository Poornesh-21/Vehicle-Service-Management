package com.albany.restapi.controller;

import com.albany.restapi.model.User;
import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.Role;
import com.albany.restapi.repository.UserRepository;
import com.albany.restapi.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody Map<String, Object> request) {
        try {
            // Check if email already exists
            String email = (String) request.get("email");
            if (email != null && userRepository.existsByEmail(email)) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "A user with this email already exists. Please use a different email address."));
            }

            // Create User entity
            User user = new User();
            user.setFirstName((String) request.get("firstName"));
            user.setLastName((String) request.get("lastName"));
            user.setEmail(email);
            user.setPhoneNumber((String) request.get("phoneNumber"));
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
            profile.setStreet((String) request.get("street"));
            profile.setCity((String) request.get("city"));
            profile.setState((String) request.get("state"));
            profile.setPostalCode((String) request.get("postalCode"));
            profile.setMembershipStatus((String) request.get("membershipStatus"));

            // Save the profile
            CustomerProfile savedProfile = customerProfileRepository.save(profile);

            // Return the created customer
            Map<String, Object> response = convertToResponseDto(savedProfile);
            response.put("tempPassword", tempPassword); // Include the temp password in response

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Integer id) {
        return customerProfileRepository.findById(id)
                .map(this::convertToResponseDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public ResponseEntity<Map<String, Object>> updateCustomer(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {

        try {
            CustomerProfile profile = customerProfileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            // Check if email is being changed to one that already exists
            String newEmail = (String) request.get("email");
            String currentEmail = profile.getUser().getEmail();

            if (newEmail != null && !newEmail.equals(currentEmail) && userRepository.existsByEmail(newEmail)) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "A user with this email already exists. Please use a different email address."));
            }

            // Update CustomerProfile
            profile.setStreet((String) request.get("street"));
            profile.setCity((String) request.get("city"));
            profile.setState((String) request.get("state"));
            profile.setPostalCode((String) request.get("postalCode"));
            profile.setMembershipStatus((String) request.get("membershipStatus"));

            // Update User
            User user = profile.getUser();
            user.setFirstName((String) request.get("firstName"));
            user.setLastName((String) request.get("lastName"));
            user.setEmail(newEmail);
            user.setPhoneNumber((String) request.get("phoneNumber"));

            // Save updates
            userRepository.save(user);
            CustomerProfile updatedProfile = customerProfileRepository.save(profile);

            return ResponseEntity.ok(convertToResponseDto(updatedProfile));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
                .orElse(ResponseEntity.notFound().build());
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