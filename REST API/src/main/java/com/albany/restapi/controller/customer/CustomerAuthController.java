package com.albany.restapi.controller.customer;

import com.albany.restapi.dto.*;
import com.albany.restapi.model.User;
import com.albany.restapi.security.JwtUtil;
import com.albany.restapi.service.customer.CustomerAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAuthController.class);

    private final CustomerAuthService customerAuthService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login/send-otp")
    public ResponseEntity<?> sendLoginOtp(@RequestBody OtpRequestDTO request) {
        try {
            logger.info("OTP request for login: {}", request.getEmail());
            customerAuthService.sendLoginOtp(request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP sent successfully to your email"
            ));
        } catch (UsernameNotFoundException e) {
            logger.warn("Login attempt for non-existent user: {}", request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Email not registered. Please sign up first.",
                    "errorField", "email"
            ));
        } catch (BadCredentialsException e) {
            logger.warn("Login attempt with invalid credentials: {}", request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "errorField", "email"
            ));
        } catch (Exception e) {
            logger.error("Error sending login OTP to {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to send OTP: " + e.getMessage(),
                    "errorField", "email"
            ));
        }
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<?> verifyLoginOtp(@RequestBody OtpVerificationDTO request) {
        try {
            logger.info("OTP verification for login: {}", request.getEmail());
            CustomerAuthResponse response = customerAuthService.verifyLoginOtp(request.getEmail(), request.getOtp());

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            logger.warn("Invalid OTP for login: {}", request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Invalid or expired OTP. Please try again.",
                    "errorField", "otp"
            ));
        } catch (Exception e) {
            logger.error("Error verifying login OTP for {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Verification failed: " + e.getMessage(),
                    "errorField", "otp"
            ));
        }
    }

    @PostMapping("/register/send-otp")
    public ResponseEntity<?> sendRegistrationOtp(@RequestBody CustomerRegistrationDTO request) {
        try {
            logger.info("OTP request for registration: {}", request.getEmail());
            customerAuthService.sendRegistrationOtp(request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP sent successfully to your email"
            ));
        } catch (IllegalArgumentException e) {
            // This handles validation errors like already registered email
            if (e.getMessage().contains("already registered")) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "errorField", "email"
                ));
            } else {
                // Other validation errors
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", e.getMessage(),
                        "errorField", determineErrorField(e.getMessage())
                ));
            }
        } catch (Exception e) {
            logger.error("Error sending registration OTP to {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Failed to send OTP: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody OtpVerificationDTO request) {
        try {
            logger.info("OTP verification for registration: {}", request.getEmail());
            CustomerAuthResponse response = customerAuthService.verifyRegistrationOtp(request.getEmail(), request.getOtp());

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            logger.warn("Invalid OTP for registration: {}", request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Invalid or expired OTP. Please try again.",
                    "errorField", "otp"
            ));
        } catch (Exception e) {
            logger.error("Error verifying registration OTP for {}: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Verification failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            String username = jwtUtil.extractUsername(token);
            User user = customerAuthService.validateCustomerToken(username, token);

            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "user", Map.of(
                            "email", user.getEmail(),
                            "firstName", user.getFirstName(),
                            "lastName", user.getLastName(),
                            "role", user.getRole().name(),
                            "membershipType", user.getMembershipType().name()
                    )
            ));
        } catch (Exception e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", "Invalid or expired token"
            ));
        }
    }

    /**
     * Helper method to determine which field caused the validation error
     */
    private String determineErrorField(String errorMessage) {
        String message = errorMessage.toLowerCase();
        if (message.contains("email") || message.contains("e-mail")) {
            return "email";
        } else if (message.contains("first name")) {
            return "firstName";
        } else if (message.contains("last name")) {
            return "lastName";
        } else if (message.contains("phone") || message.contains("mobile")) {
            return "phoneNumber";
        } else if (message.contains("password")) {
            return "password";
        } else {
            return "general";
        }
    }
}