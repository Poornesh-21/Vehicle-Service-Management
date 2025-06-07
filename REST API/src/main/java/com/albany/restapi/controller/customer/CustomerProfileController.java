package com.albany.restapi.controller.customer;

import com.albany.restapi.dto.CustomerProfileUpdateDTO;
import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.CustomerRepository;
import com.albany.restapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/profile")
public class CustomerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProfileController.class);

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public CustomerProfileController(CustomerRepository customerRepository, UserRepository userRepository) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get current customer profile
     */
    @GetMapping
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            CustomerProfile profile = customerRepository.findByUser_UserId(user.getUserId())
                    .orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("isActive", user.isActive());
            response.put("role", user.getRole().name());
            response.put("membershipType", user.getMembershipType().name());

            if (user.getMembershipStartDate() != null) {
                response.put("membershipStartDate", user.getMembershipStartDate());
            }

            if (user.getMembershipEndDate() != null) {
                response.put("membershipEndDate", user.getMembershipEndDate());
                boolean isExpired = user.getMembershipEndDate().isBefore(LocalDateTime.now());
                response.put("isMembershipExpired", isExpired);

                if (!isExpired) {
                    long daysRemaining = java.time.Duration.between(
                            LocalDateTime.now(),
                            user.getMembershipEndDate()
                    ).toDays();
                    response.put("membershipDaysRemaining", daysRemaining);
                }
            }

            if (profile != null) {
                response.put("customerId", profile.getCustomerId());
                response.put("street", user.getStreet() != null ? user.getStreet() : profile.getStreet());
                response.put("city", user.getCity() != null ? user.getCity() : profile.getCity());
                response.put("state", user.getState() != null ? user.getState() : profile.getState());
                response.put("postalCode", user.getPostalCode() != null ? user.getPostalCode() : profile.getPostalCode());
                response.put("membershipStatus", profile.getMembershipStatus());
                response.put("totalServices", profile.getTotalServices());
                response.put("lastServiceDate", profile.getLastServiceDate());
            } else {
                response.put("street", user.getStreet());
                response.put("city", user.getCity());
                response.put("state", user.getState());
                response.put("postalCode", user.getPostalCode());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching customer profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    /**
     * Update customer profile
     */
    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody CustomerProfileUpdateDTO updateDTO,
                                           Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            CustomerProfile profile = customerRepository.findByUser_UserId(user.getUserId())
                    .orElse(null);

            // Update user details
            if (updateDTO.getFirstName() != null && !updateDTO.getFirstName().isEmpty()) {
                user.setFirstName(updateDTO.getFirstName());
            }

            if (updateDTO.getLastName() != null && !updateDTO.getLastName().isEmpty()) {
                user.setLastName(updateDTO.getLastName());
            }

            // Update address information
            user.setStreet(updateDTO.getStreet());
            user.setCity(updateDTO.getCity());
            user.setState(updateDTO.getState());
            user.setPostalCode(updateDTO.getPostalCode());

            userRepository.save(user);

            // Update customer profile if it exists
            if (profile != null) {
                profile.setStreet(updateDTO.getStreet());
                profile.setCity(updateDTO.getCity());
                profile.setState(updateDTO.getState());
                profile.setPostalCode(updateDTO.getPostalCode());

                customerRepository.save(profile);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error updating customer profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }
}