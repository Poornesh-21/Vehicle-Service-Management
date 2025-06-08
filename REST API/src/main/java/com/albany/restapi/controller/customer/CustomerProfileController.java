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

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody CustomerProfileUpdateDTO updateDTO,
                                           Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            logger.info("Updating profile for user ID: {}, email: {}", user.getUserId(), user.getEmail());
            
            CustomerProfile profile = customerRepository.findByUser_UserId(user.getUserId())
                    .orElse(null);

            // Update user details
            boolean updated = false;
            
            if (updateDTO.getFirstName() != null && !updateDTO.getFirstName().isEmpty()) {
                user.setFirstName(updateDTO.getFirstName());
                updated = true;
                logger.debug("Updated firstName to: {}", updateDTO.getFirstName());
            }

            if (updateDTO.getLastName() != null && !updateDTO.getLastName().isEmpty()) {
                user.setLastName(updateDTO.getLastName());
                updated = true;
                logger.debug("Updated lastName to: {}", updateDTO.getLastName());
            }
            
            // Update phone number if provided
            if (updateDTO.getPhoneNumber() != null) {
                user.setPhoneNumber(updateDTO.getPhoneNumber());
                updated = true;
                logger.debug("Updated phoneNumber to: {}", updateDTO.getPhoneNumber());
            }

            // Update address information
            if (updateDTO.getStreet() != null) {
                user.setStreet(updateDTO.getStreet());
                updated = true;
                logger.debug("Updated street to: {}", updateDTO.getStreet());
            }
            
            if (updateDTO.getCity() != null) {
                user.setCity(updateDTO.getCity());
                updated = true;
                logger.debug("Updated city to: {}", updateDTO.getCity());
            }
            
            if (updateDTO.getState() != null) {
                user.setState(updateDTO.getState());
                updated = true;
                logger.debug("Updated state to: {}", updateDTO.getState());
            }
            
            if (updateDTO.getPostalCode() != null) {
                user.setPostalCode(updateDTO.getPostalCode());
                updated = true;
                logger.debug("Updated postalCode to: {}", updateDTO.getPostalCode());
            }

            if (updated) {
                userRepository.save(user);
                logger.info("User details updated successfully for user ID: {}", user.getUserId());
            } else {
                logger.info("No changes detected for user ID: {}", user.getUserId());
            }

            // Update customer profile if it exists
            if (profile != null) {
                if (updateDTO.getStreet() != null) {
                    profile.setStreet(updateDTO.getStreet());
                }
                
                if (updateDTO.getCity() != null) {
                    profile.setCity(updateDTO.getCity());
                }
                
                if (updateDTO.getState() != null) {
                    profile.setState(updateDTO.getState());
                }
                
                if (updateDTO.getPostalCode() != null) {
                    profile.setPostalCode(updateDTO.getPostalCode());
                }

                customerRepository.save(profile);
                logger.info("Customer profile updated successfully for customer ID: {}", profile.getCustomerId());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully"
            ));
        } catch (Exception e) {
            logger.error("Error updating customer profile: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }
}