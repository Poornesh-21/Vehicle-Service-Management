package com.albany.restapi.controller.customer;

import com.albany.restapi.model.CustomerProfile;
import com.albany.restapi.model.User;
import com.albany.restapi.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for customer profile operations
 */
@RestController
@RequestMapping("/api/customer")
public class CustomerProfileController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerProfileController.class);

    private final CustomerRepository customerRepository;

    public CustomerProfileController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Get current customer profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        try {
            // Get authenticated user
            User user = (User) authentication.getPrincipal();
            
            // Get customer profile
            Optional<CustomerProfile> customerProfileOpt = customerRepository.findByUser_UserId(user.getUserId());
            
            // Create response with user data
            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getUserId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("phoneNumber", user.getPhoneNumber());
            response.put("isActive", user.isActive());
            response.put("role", user.getRole().name());
            response.put("membershipType", user.getMembershipType().name());
            
            // Add membership dates if available
            if (user.getMembershipStartDate() != null) {
                response.put("membershipStartDate", user.getMembershipStartDate());
            }
            
            if (user.getMembershipEndDate() != null) {
                response.put("membershipEndDate", user.getMembershipEndDate());
                
                // Check if membership is expired
                boolean isExpired = user.getMembershipEndDate().isBefore(LocalDateTime.now());
                response.put("isMembershipExpired", isExpired);
                
                // Calculate days remaining if not expired
                if (!isExpired) {
                    long daysRemaining = java.time.Duration.between(
                            LocalDateTime.now(), 
                            user.getMembershipEndDate()
                    ).toDays();
                    response.put("membershipDaysRemaining", daysRemaining);
                }
            }
            
            // Add customer profile data if available
            if (customerProfileOpt.isPresent()) {
                CustomerProfile profile = customerProfileOpt.get();
                response.put("customerId", profile.getCustomerId());
                response.put("street", user.getStreet() != null ? user.getStreet() : profile.getStreet());
                response.put("city", user.getCity() != null ? user.getCity() : profile.getCity());
                response.put("state", user.getState() != null ? user.getState() : profile.getState());
                response.put("postalCode", user.getPostalCode() != null ? user.getPostalCode() : profile.getPostalCode());
                response.put("membershipStatus", profile.getMembershipStatus());
                response.put("totalServices", profile.getTotalServices());
                response.put("lastServiceDate", profile.getLastServiceDate());
            } else {
                // Add address data from user if available
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
}